package ru.practicum.service.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.client.StatClient;
import ru.practicum.dto.request.StatHitRequestDto;
import ru.practicum.dto.response.HitsCounterResponseDto;
import ru.practicum.service.dal.CompilationRepository;
import ru.practicum.service.dal.EventRepository;
import ru.practicum.service.dto.*;
import ru.practicum.service.error.ConflictException;
import ru.practicum.service.error.NotFoundException;
import ru.practicum.service.mapper.CompilationMapper;
import ru.practicum.service.mapper.EventMapper;
import ru.practicum.service.model.Compilation;
import ru.practicum.service.model.Event;
import ru.practicum.service.statistics.StatisticsService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class CompilationService {
	final CompilationRepository compilationRepository;
	final StatClient statClient;
	final StatisticsService statisticsService;
	final EventRepository eventRepository;

	public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size, HttpServletRequest request) {
		log.info("PublicCompilationService: выгрузка подборок по заданным параметрам");
		List<Compilation> compilationsList = compilationRepository.findCompilations(pinned, from, size);
		log.info("{}", compilationsList);

		Set<String> eventUris = compilationsList.stream()
				.flatMap(comp -> comp.getEvents().stream())
				.map(event -> "/events/" + event.getId())
				.collect(Collectors.toSet());

		List<HitsCounterResponseDto> stats = statClient.getHits(eventUris.stream().toList(), false);

		Map<Long, Long> eventViews = stats.stream()
				.collect(Collectors.toMap(
						dto -> EventMapper.extractIdFromUri(dto.getUri()),
						HitsCounterResponseDto::getHits)
				);

		List<CompilationDto> result = new ArrayList<>();
		for (Compilation comp : compilationsList) {
			Set<EventShortDto> eventDtos = comp.getEvents().stream()
					.map(event -> EventMapper.toEventShortDto(event, eventViews.getOrDefault(event.getId(), 0L)))
					.collect(Collectors.toSet());

			CompilationDto compilationDto = CompilationMapper.toCompilationDto(comp, eventDtos);

			result.add(compilationDto);
		}

		statClient.hit(new StatHitRequestDto(Constant.SERVICE_POSTFIX,
				request.getRequestURI(),
				request.getRemoteAddr(),
				LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constant.DATE_TIME_FORMAT)))
		);

		return result;
	}

	public CompilationDto getCompilationById(Long compId, HttpServletRequest request) {
		log.info("PublicCompilationService: поиск подборки с id: {}", compId);
		Compilation compilation = compilationRepository.findById(compId)
				.orElseThrow(() -> new NotFoundException(String.format("Подборка с id: %d не найдена", compId)));

		Set<EventShortDto> eventShortDtoList = statisticsService.getEventShortDto(compilation.getEvents(), false);

		statClient.hit(new StatHitRequestDto(Constant.SERVICE_POSTFIX,
				request.getRequestURI(),
				request.getRemoteAddr(),
				LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constant.DATE_TIME_FORMAT)))
		);

		return CompilationMapper.toCompilationDto(compilation, eventShortDtoList);
	}

	public CompilationDto createCompilation(NewCompilationDto dto) {
		if (compilationRepository.existsByTitle(dto.getTitle())) {
			throw new ConflictException("Подборка событий с названием " +  dto.getTitle() + " уже существует!");
		}

		Compilation compilation = CompilationMapper.newCompilationToEntity(dto);

		Set<EventShortDto> eventShortDtos = Collections.emptySet();

		if (dto.getEventsIds() != null && !dto.getEventsIds().isEmpty()) {
			Set<Event> events = new HashSet<>(eventRepository.findAllByIdIn(dto.getEventsIds()));
			if (events.size() != dto.getEventsIds().size()) {
				throw new NotFoundException("Некоторые события не были найдены.");
			}
			eventShortDtos = statisticsService.getEventShortDto(events, false);
			compilation.setEvents(events);
		}

		Compilation savedCompilation = compilationRepository.save(compilation);

		return CompilationMapper.toCompilationDto(savedCompilation, eventShortDtos);
	}

	public void deleteCompilation(Long id) {
		if (!compilationRepository.existsById(id)) {
			throw new NotFoundException("Подборка событий с id=" + id + " не найдена.");
		}
		compilationRepository.deleteById(id);
	}

	public CompilationDto updateCompilation(Long id, UpdateCompilationRequest request) {
		Compilation compilation = compilationRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Подборка событий с id=" + id + " не найдена."));

		if (request.getTitle() != null) {
			if (!request.getTitle().equals(compilation.getTitle()) &&
					compilationRepository.existsByTitle(request.getTitle())) {
				throw new ConflictException("Подборка событий с названием " + request.getTitle() + " уже существует!");
			}
			compilation.setTitle(request.getTitle());
		}

		if (request.getPinned() != null) {
			compilation.setPinned(request.getPinned());
		}

		Set<Event> events = compilation.getEvents();

		if (request.getEventsIds() != null) {
			if (request.getEventsIds().isEmpty()) {
				events = Collections.emptySet();
			} else {
				events = new HashSet<>(eventRepository.findAllByIdIn(request.getEventsIds()));
				if (events.size() != request.getEventsIds().size()) {
					throw new NotFoundException("Некоторые события не были найдены.");
				}
			}
			compilation.setEvents(events);
		}

		Compilation savedCompilation = compilationRepository.save(compilation);

		Set<EventShortDto> eventShortDtos = events != null && !events.isEmpty()
				? statisticsService.getEventShortDto(events, false)
				: Collections.emptySet();

		return CompilationMapper.toCompilationDto(savedCompilation, eventShortDtos);
	}
}
