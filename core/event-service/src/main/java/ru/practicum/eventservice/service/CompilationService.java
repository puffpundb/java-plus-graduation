package ru.practicum.eventservice.service;

import io.github.resilience4j.retry.annotation.Retry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.client.StatClient;
import ru.practicum.dto.request.StatHitRequestDto;
import ru.practicum.dto.response.HitsCounterResponseDto;
import ru.practicum.eventservice.entity.Compilation;
import ru.practicum.eventservice.entity.Event;
import ru.practicum.eventservice.mapper.CompilationMapper;
import ru.practicum.eventservice.mapper.EventMapper;
import ru.practicum.eventservice.repository.CompilationRepository;
import ru.practicum.eventservice.repository.EventRepository;
import ru.practicum.iteractionapi.dto.Constant;
import ru.practicum.iteractionapi.dto.event.CompilationDto;
import ru.practicum.iteractionapi.dto.event.EventShortDto;
import ru.practicum.iteractionapi.dto.event.NewCompilationDto;
import ru.practicum.iteractionapi.dto.event.UpdateCompilationRequest;
import ru.practicum.iteractionapi.dto.user.UserDto;
import ru.practicum.iteractionapi.error.ConflictException;
import ru.practicum.iteractionapi.error.NotFoundException;
import ru.practicum.iteractionapi.feignapi.requestfeignclient.InternalRequestsFeignClient;
import ru.practicum.iteractionapi.feignapi.userfeignclient.UserFeignClient;
import ru.practicum.iteractionapi.statistics.StatisticsService;

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
	final StatisticsService statisticsService;
	final StatClient statClient;
	final EventRepository eventRepository;

	final UserFeignClient userFeignClient;
	final InternalRequestsFeignClient internalRequestsFeignClient;

	final String URI_EVENT_ENDPOINT = "/events/";

	@Retry(name = "eventServiceRetry", fallbackMethod = "fallbackFindUsers")
	protected List<UserDto> findUsers(List<Long> ids, int from, int size) {
		return userFeignClient.findUsers(ids, from, size);
	}

	private List<UserDto> fallbackFindUsers(List<Long> ids, int from, int size, Throwable t) {
		log.warn("User-service недоступен, возвращаем пустой список. Причина: {}", t.getMessage());
		return Collections.emptyList();
	}

	@Retry(name = "eventServiceRetry", fallbackMethod = "fallbackGetConfirmedRequestsCounts")
	protected Map<Long, Long> getConfirmedRequestsCounts(List<Long> eventIds) {
		return internalRequestsFeignClient.getConfirmedRequestsCounts(eventIds);
	}

	private Map<Long, Long> fallbackGetConfirmedRequestsCounts(List<Long> eventIds, Throwable t) {
		log.warn("Request-service недоступен, возвращаем 0 для eventIds: {}. Причина: {}", eventIds, t.getMessage());
		if (eventIds == null || eventIds.isEmpty()) return Map.of();
		return eventIds.stream().collect(Collectors.toMap(id -> id, id -> 0L));
	}

	@Retry(name = "eventServiceRetry", fallbackMethod = "fallbackGetConfirmedRequestsCount")
	protected Long getConfirmedRequestsCount(Long eventId) {
		return internalRequestsFeignClient.getConfirmedRequestsCount(eventId);
	}

	private Long fallbackGetConfirmedRequestsCount(Long eventId, Throwable t) {
		log.warn("Request-service недоступен, возвращаем 0 для eventId: {}", eventId);
		return 0L;
	}

	public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size, HttpServletRequest request) {
		log.info("PublicCompilationService: выгрузка подборок по заданным параметрам");
		List<Compilation> compilationsList = compilationRepository.findCompilations(pinned, from, size);
		if (compilationsList.isEmpty()) {
			return Collections.emptyList();
		}

		Set<Event> allEvents = compilationsList.stream()
				.flatMap(comp -> comp.getEvents().stream())
				.collect(Collectors.toSet());

		if (allEvents.isEmpty()) {
			return Collections.emptyList();
		}

		List<Long> initiatorsIds = allEvents.stream()
				.map(Event::getInitiatorId)
				.distinct()
				.toList();

		List<UserDto> users = findUsers(initiatorsIds, 0, initiatorsIds.size());
		if (users == null || users.isEmpty()) {
			log.warn("Не удалось получить пользователей, возвращаем пустой список подборок");
			return Collections.emptyList();
		}
		Map<Long, UserDto> userMap = users.stream()
				.collect(Collectors.toMap(UserDto::getId, u -> u));

		Set<String> eventUris = allEvents.stream()
				.map(event -> "/events/" + event.getId())
				.collect(Collectors.toSet());
		List<HitsCounterResponseDto> stats = statClient.getHits(new ArrayList<>(eventUris), false);
		Map<Long, Long> eventViews = stats.stream()
				.collect(Collectors.toMap(
						dto -> EventMapper.extractIdFromUri(dto.getUri()),
						HitsCounterResponseDto::getHits));

		List<Long> allEventIds = allEvents.stream().map(Event::getId).toList();
		Map<Long, Long> confirmedMap = getConfirmedRequestsCounts(allEventIds);

		List<CompilationDto> result = new ArrayList<>();
		for (Compilation comp : compilationsList) {
			Set<EventShortDto> eventDtos = comp.getEvents().stream()
					.map(event -> {
						UserDto initiator = userMap.get(event.getInitiatorId());
						if (initiator == null) {
							log.warn("Инициатор не найден для события {}", event.getId());
							return null;
						}
						Long views = eventViews.getOrDefault(event.getId(), 0L);
						Long confirmed = confirmedMap.getOrDefault(event.getId(), 0L);
						return EventMapper.toEventShortDto(event, initiator, confirmed, views);
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());

			if (eventDtos.isEmpty() && !comp.getEvents().isEmpty()) {
				continue;
			}
			result.add(CompilationMapper.toCompilationDto(comp, eventDtos));
		}

		statClient.hit(new StatHitRequestDto(Constant.SERVICE_POSTFIX,
				request.getRequestURI(),
				request.getRemoteAddr(),
				LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constant.DATE_TIME_FORMAT)))
		);

		return result;
	}

	private Set<EventShortDto> setViewsToEventShortDto(Compilation compilation) {
		Set<Event> events = compilation.getEvents();
		if (events.isEmpty()) {
			return Collections.emptySet();
		}

		List<Long> initiatorIds = events.stream()
				.map(Event::getInitiatorId)
				.distinct()
				.toList();

		List<UserDto> users = findUsers(initiatorIds, 0, initiatorIds.size());
		if (users == null || users.isEmpty()) {
			log.warn("Не удалось получить пользователей для подборки {}", compilation.getId());
			return Collections.emptySet();
		}

		Map<Long, UserDto> userMap = users.stream()
				.collect(Collectors.toMap(UserDto::getId, u -> u));

		List<Long> allEventIds = events.stream().map(Event::getId).toList();
		Map<Long, Long> confirmedMap = getConfirmedRequestsCounts(allEventIds);

		Set<Long> eventsIdSet = new HashSet<>(allEventIds);
		Map<String, Long> eventIdEventHits = statisticsService.getEventShortDto(eventsIdSet, false);

		return events.stream()
				.map(event -> {
					UserDto initiator = userMap.get(event.getInitiatorId());
					if (initiator == null) {
						log.warn("Инициатор не найден для события {}", event.getId());
						return null;
					}
					Long views = eventIdEventHits.getOrDefault(URI_EVENT_ENDPOINT + event.getId(), 0L);
					Long confirmed = confirmedMap.getOrDefault(event.getId(), 0L);
					return EventMapper.toEventShortDto(event, initiator, confirmed, views);
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	public CompilationDto getCompilationById(Long compId, HttpServletRequest request) {
		log.info("PublicCompilationService: поиск подборки с id: {}", compId);
		Compilation compilation = compilationRepository.findById(compId)
				.orElseThrow(() -> new NotFoundException(String.format("Подборка с id: %d не найдена", compId)));

		Set<EventShortDto> eventShortDtoList = setViewsToEventShortDto(compilation);

		statClient.hit(new StatHitRequestDto(Constant.SERVICE_POSTFIX,
				request.getRequestURI(),
				request.getRemoteAddr(),
				LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constant.DATE_TIME_FORMAT)))
		);

		return CompilationMapper.toCompilationDto(compilation, eventShortDtoList);
	}

	public CompilationDto createCompilation(NewCompilationDto dto) {
		if (compilationRepository.existsByTitle(dto.getTitle())) {
			throw new ConflictException("Подборка событий с названием " + dto.getTitle() + " уже существует!");
		}

		Compilation compilation = CompilationMapper.newCompilationToEntity(dto);

		Set<EventShortDto> eventShortDtos = Collections.emptySet();

		if (dto.getEventsIds() != null && !dto.getEventsIds().isEmpty()) {
			Set<Event> events = new HashSet<>(eventRepository.findAllByIdIn(dto.getEventsIds()));
			if (events.size() != dto.getEventsIds().size()) {
				throw new NotFoundException("Некоторые события не были найдены.");
			}
			compilation.setEvents(events);
			eventShortDtos = setViewsToEventShortDto(compilation);
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

		Set<EventShortDto> eventShortDtos;
		if (events != null && !events.isEmpty()) {
			eventShortDtos = setViewsToEventShortDto(savedCompilation);
		} else {
			eventShortDtos = Collections.emptySet();
		}

		return CompilationMapper.toCompilationDto(savedCompilation, eventShortDtos);
	}
}
