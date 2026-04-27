package ru.practicum.eventservice.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.StatClient;
import ru.practicum.dto.request.StatHitRequestDto;
import ru.practicum.dto.response.HitsCounterResponseDto;
import ru.practicum.eventservice.entity.Category;
import ru.practicum.eventservice.entity.Event;
import ru.practicum.eventservice.entity.Location;
import ru.practicum.eventservice.mapper.EventMapper;
import ru.practicum.eventservice.repository.CategoryRepository;
import ru.practicum.eventservice.repository.EventRepository;
import ru.practicum.iteractionapi.dto.Constant;
import ru.practicum.iteractionapi.dto.enums.AdminStateAction;
import ru.practicum.iteractionapi.dto.event.*;
import ru.practicum.iteractionapi.dto.user.UserDto;
import ru.practicum.iteractionapi.error.ConflictException;
import ru.practicum.iteractionapi.error.NotFoundException;
import ru.practicum.iteractionapi.error.ValidationException;
import ru.practicum.iteractionapi.feignapi.userfeignclient.UserFeignClient;
import ru.practicum.iteractionapi.model.enums.State;
import ru.practicum.iteractionapi.statistics.StatisticsService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class EventService {
	final EventRepository eventRepository;
	final CategoryRepository categoryRepository;

	final StatClient statClient;
	final StatisticsService statsService;

	final UserFeignClient userFeignClient;

	static final String URI_EVENT_ENDPOINT = "/events/";

	public List<EventShortDto> getEvents(String text,
										 List<Long> categories,
										 Boolean paid,
										 LocalDateTime rangeStart,
										 LocalDateTime rangeEnd,
										 Boolean onlyAvailable,
										 EventSort sort,
										 Integer from,
										 Integer size,
										 HttpServletRequest request) {
		if (rangeStart == null && rangeEnd == null) rangeStart = LocalDateTime.now();
		if (rangeEnd == null) rangeEnd = LocalDateTime.now().plusYears(1000);

		log.info("PublicEventService: Поиск ивентов с заданными параметрами");
		Pageable pageable = PageRequest.of(from / size, size);
		List<Event> eventsList = eventRepository.findPublicEvents(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, pageable);

		if (eventsList.isEmpty()) {
			return Collections.emptyList();
		}

		List<Long> initiatorIds = eventsList.stream().map(Event::getInitiator).distinct().toList();
		List<UserDto> users = userFeignClient.findUsers(initiatorIds, 0, initiatorIds.size());
		Map<Long, UserDto> userMap = users.stream()
				.collect(Collectors.toMap(UserDto::getId, Function.identity()));

		LocalDateTime startStat = eventsList.stream()
				.map(Event::getPublishedOn)
				.filter(Objects::nonNull)
				.min(LocalDateTime::compareTo)
				.orElseThrow(() -> new IllegalStateException("У событий отсутствует дата публикации"));
		LocalDateTime statEnd = LocalDateTime.now();

		log.info("PublicEventService: {}", eventsList);
		List<String> eventsUrisList = eventsList.stream().map(event -> URI_EVENT_ENDPOINT + event.getId()).toList();

		log.info("PublicEventService: Выгрузка статистики по найденным ивентам");
		List<HitsCounterResponseDto> hitsCounterList = statClient.getHits(startStat, statEnd, eventsUrisList, false);
		log.info("PublicEventService: {}", hitsCounterList);
		Map<Long, Long> eventIdEventHits =  hitsCounterList.stream()
				.collect(Collectors.toMap(hitsCounter ->
						EventMapper.extractIdFromUri(hitsCounter.getUri()), HitsCounterResponseDto::getHits));

		List<EventShortDto> result = eventsList.stream()
				.map(event -> {
					UserDto initiator = userMap.get(event.getInitiator());
					Long views = eventIdEventHits.getOrDefault(event.getId(), 0L);
					return EventMapper.toEventShortDto(event, initiator, views);
				})
				.toList();

		if (sort == EventSort.VIEWS) result = result.stream()
				.sorted(Comparator.comparingLong(EventShortDto::getViews)
						.reversed()).toList();

		if (request != null) {
			statClient.hit(new StatHitRequestDto(Constant.SERVICE_POSTFIX,
					request.getRequestURI(),
					request.getRemoteAddr(),
					LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constant.DATE_TIME_FORMAT)))
			);
		}

		return result;
	}

	public EventFullDto getById(Long id, HttpServletRequest request) {
		log.info("PublicEventService: Поиск ивента с переданным id: {}", id);
		Event event = eventRepository.findPublishedById(id)
				.orElseThrow(() -> new NotFoundException(String.format("Событие с id: %d не найдено", id)));

		log.info("PublicEventService: Выгрузка статистики по найденному ивенту");

		statClient.hit(new StatHitRequestDto(Constant.SERVICE_POSTFIX,
				request.getRequestURI(),
				request.getRemoteAddr(),
				LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constant.DATE_TIME_FORMAT)))
		);

		List<HitsCounterResponseDto> hitsCounter = statClient.getHits(event.getPublishedOn(),
				LocalDateTime.now(),
				List.of(URI_EVENT_ENDPOINT + event.getId()),
				true);
		Long views = hitsCounter.isEmpty() ? 0L : hitsCounter.getFirst().getHits();

		UserDto userDto = userFeignClient.findUsers(List.of(event.getInitiator()), 0 , 1).getFirst();

		return EventMapper.toEventFullDto(event, userDto, views);
	}

	private Map<String, Long> getViewsForEvents(List<EventShortDto> events) {
		if (events.isEmpty()) {
			return Collections.emptyMap();
		}
		List<String> uris = events.stream()
				.map(dto -> URI_EVENT_ENDPOINT + dto.getId())
				.collect(Collectors.toList());
		return statsService.getViewsByUris(uris, false);
	}

	public List<EventShortDto> getEventsByOwner(Long userId, Long from, Long size) {
		int page = from.intValue() / size.intValue();
		Pageable pageable = PageRequest.of(page, size.intValue());

		Page<Event> eventPage = eventRepository.findByInitiator_Id(userId, pageable);

		UserDto initiator = userFeignClient.findUsers(List.of(userId), 0, 1).getFirst();

		List<EventShortDto> dtos = eventPage.getContent().stream()
				.map(event -> EventMapper.toEventShortDto(event, initiator, 0L))
				.collect(Collectors.toList());

		Map<String, Long> viewsMap = getViewsForEvents(dtos);

		dtos.forEach(dto -> {
			String uriKey = URI_EVENT_ENDPOINT + dto.getId();
			Long views = viewsMap.getOrDefault(uriKey, 0L);
			dto.setViews(views);
		});

		return dtos;
	}

	@Transactional
	public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
		if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
			throw new ValidationException("Время события должно быть за два часа до события.");
		}

		Category category = categoryRepository.findById(newEventDto.getCategoryId())
				.orElseThrow(() -> new ValidationException("Категория не указана"));

		List<UserDto> userList = userFeignClient.findUsers(List.of(userId), 0, 1);
		if (userList.isEmpty()) throw new NotFoundException("Такого пользователя не существует.");

		Event event = eventRepository.save(EventMapper.newEventDtoToEvent(newEventDto, userId, category));

		return EventMapper.eventToEventFullDto(event, userList.getFirst());
	}

	public EventFullDto getInfoEvent(Long userId, Long eventId) {
		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new NotFoundException("Такого события не найдено."));

		List<UserDto> userList = userFeignClient.findUsers(List.of(userId), 0, 1);
		if (userList.isEmpty()) throw new NotFoundException("Такого пользователя не существует.");

		EventFullDto eventFullDto = EventMapper.eventToEventFullDto(event, userList.getFirst());

		List<HitsCounterResponseDto> hitsCounter = statClient.getHits(
				List.of(URI_EVENT_ENDPOINT + eventFullDto.getId()),
				true);
		Long views = hitsCounter.isEmpty() ? 0L : hitsCounter.getFirst().getHits();

		eventFullDto.setViews(views);
		return eventFullDto;
	}

	@Transactional
	public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new NotFoundException("Такого события не найдено."));

		List<UserDto> userList = userFeignClient.findUsers(List.of(userId), 0, 1);
		if (userList.isEmpty()) throw new NotFoundException("Такого пользователя не существует.");

		if (event.getState().equals(State.PUBLISHED)) {
			throw new ConflictException("Данное событие нельзя изменить.");
		}
		Optional<Category> category = Optional.empty();
		if (updateEventUserRequest.getCategoryId() != null) {
			category = categoryRepository.findById(updateEventUserRequest.getCategoryId());
		}
		Event updateEvent = eventRepository
				.save(EventMapper.updateEventDtoToEvent(event, updateEventUserRequest, category));

		return EventMapper.eventToEventFullDto(updateEvent, userList.getFirst());
	}

	public List<EventFullDto> getFullEvents(AdminEventParam params) {
		List<State> states = convertStatesEnum(params.getStates());

		int pageNumber = params.getFrom() / params.getSize();
		Pageable pageable = PageRequest.of(pageNumber, params.getSize());

		List<Event> events = eventRepository.findEventByAdmin(
				params.getUsers(),
				states,
				params.getCategories(),
				params.getRangeStart(),
				params.getRangeEnd(),
				pageable
		);

		List<Long> initiatorIds = events.stream()
				.map(Event::getInitiator)
				.distinct()
				.toList();

		List<UserDto> users = userFeignClient.findUsers(initiatorIds, 0, initiatorIds.size());
		Map<Long, UserDto> userMap = users.stream()
				.collect(Collectors.toMap(UserDto::getId, Function.identity()));

		List<String> uris = events.stream()
				.map(event -> URI_EVENT_ENDPOINT + event.getId())
				.toList();

		Map<String, Long> eventHits = statsService.getViewsByUris(uris, false);

		return events.stream()
				.map(event -> {
					Long views = eventHits.getOrDefault(URI_EVENT_ENDPOINT + event.getId(), 0L);
					UserDto initiator = userMap.get(event.getInitiator());
					if (initiator == null) {
						throw new NotFoundException("User not found: " + event.getInitiator());
					}
					return EventMapper.toEventFullDto(event, initiator, views);
				})
				.toList();
	}

	private List<State> convertStatesEnum(List<String> states) {
		if (states == null || states.isEmpty()) {
			return null;
		}
		return states.stream()
				.map(state -> {
					try {
						return State.valueOf(state.toUpperCase());
					} catch (IllegalArgumentException e) {
						throw new ValidationException("Некорректное состояние события: " + state);
					}
				})
				.toList();
	}

	@Transactional
	public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено."));

		if (request.getStateAction() != null) {

			if (request.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
				if (event.getState() != State.PENDING) {
					throw new ConflictException("Cобытие можно публиковать, только если оно в состоянии ожидания публикации");
				}
				event.setState(State.PUBLISHED);
				event.setPublishedOn(LocalDateTime.now());

			} else if (request.getStateAction() == AdminStateAction.REJECT_EVENT) {
				if (event.getState() == State.PUBLISHED) {
					throw new ConflictException("Cобытие можно отклонить, только если оно еще не опубликовано");
				}
				event.setState(State.CANCELED);
			}
		}

		updateEventFields(event, request);
		Event updatedEvent = eventRepository.save(event);

		List<UserDto> users = userFeignClient.findUsers(List.of(updatedEvent.getInitiator()), 0, 1);
		if (users.isEmpty()) {
			throw new NotFoundException("Пользователь не найден: " + updatedEvent.getInitiator());
		}
		UserDto initiator = users.getFirst();

		String uri = URI_EVENT_ENDPOINT + event.getId();
		Long views = statsService.getViewsByUri(uri,false);

		return EventMapper.toEventFullDto(updatedEvent, initiator, views);
	}

	private void updateEventFields(Event event, UpdateEventAdminRequest request) {
		if (request.getAnnotation() != null) {
			event.setAnnotation(request.getAnnotation());
		}

		if (request.getCategoryId() != null) {
			Category category = categoryRepository.findById(request.getCategoryId())
					.orElseThrow(() -> new NotFoundException("Категория с id=" + request.getCategoryId() + " не найдена."));
			event.setCategory(category);
		}

		if (request.getDescription() != null) {
			event.setDescription(request.getDescription());
		}

		if (request.getEventDate() != null) {
			event.setEventDate(request.getEventDate());
		}

		if (request.getLocationDto() != null) {
			event.setLocation(
					Location.builder()
							.lat(request.getLocationDto().getLat())
							.lon(request.getLocationDto().getLon())
							.build());
		}

		if (request.getPaid() != null) {
			event.setPaid(request.getPaid());
		}

		if (request.getParticipantLimit() != null) {
			event.setParticipantLimit(request.getParticipantLimit());
		}

		if (request.getRequestModeration() != null) {
			event.setRequestModeration(request.getRequestModeration());
		}

		if (request.getTitle() != null) {
			event.setTitle(request.getTitle());
		}
	}

	@Transactional
	public void setConfirmedRequests(Long eventId, Long confirmedRequests) {
		int updated = eventRepository.updateConfirmedRequests(eventId, confirmedRequests);
		if (updated == 0) {
			throw new NotFoundException("Event not found");
		}
	}
}
