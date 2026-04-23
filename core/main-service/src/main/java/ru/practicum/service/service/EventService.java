package ru.practicum.service.service;

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
import ru.practicum.service.dto.AdminEventParam;
import ru.practicum.service.dal.CategoryRepository;
import ru.practicum.service.dal.EventRepository;
import ru.practicum.service.dal.RequestRepository;
import ru.practicum.service.dal.UserRepository;
import ru.practicum.service.dto.*;
import ru.practicum.service.dto.enums.AdminStateAction;
import ru.practicum.service.error.ConflictException;
import ru.practicum.service.error.NotFoundException;
import ru.practicum.service.error.ValidationException;
import ru.practicum.service.mapper.EventMapper;
import ru.practicum.service.mapper.RequestMapper;
import ru.practicum.service.model.*;
import ru.practicum.service.model.enums.State;
import ru.practicum.service.model.enums.Status;
import ru.practicum.service.statistics.StatisticsService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class EventService {
	final EventRepository eventRepository;
	final StatClient statClient;
	private final StatClient client;
	private final StatisticsService statsService;
	private final CategoryRepository categoryRepository;
	private final UserRepository userRepository;
	private final RequestRepository requestRepository;
	private final StatisticsService  statisticsService;

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
				.map(event -> EventMapper.toEventShortDto(event, eventIdEventHits.getOrDefault(event.getId(), 0L)))
				.toList();

		if (sort == EventSort.VIEWS) result = result.stream()
				.sorted(Comparator.comparingLong(EventShortDto::getViews)
						.reversed()).toList();

		statClient.hit(new StatHitRequestDto(Constant.SERVICE_POSTFIX,
				request.getRequestURI(),
				request.getRemoteAddr(),
				LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constant.DATE_TIME_FORMAT)))
		);

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

		return EventMapper.toEventFullDto(event, views);
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
		List<EventShortDto> dtos = eventPage.getContent().stream()
				.map(EventMapper::toEventShortDto)
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

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("Такого пользователя не существует."));

		Event event = eventRepository.save(EventMapper.newEventDtoToEvent(newEventDto, user, category));

		return EventMapper.eventToEventFullDto(event);
	}

	public EventFullDto getInfoEvent(Long userId, Long eventId) {
		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new NotFoundException("Такого события не найдено."));
		userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("Такого пользователя не существует."));
		EventFullDto eventFullDto = EventMapper.eventToEventFullDto(event);

		List<HitsCounterResponseDto> hitsCounter = client.getHits(
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
		userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("Такого пользователя не существует."));
		if (event.getState().equals(State.PUBLISHED)) {
			throw new ConflictException("Данное событие нельзя изменить.");
		}
		Optional<Category> category = Optional.empty();
		if (updateEventUserRequest.getCategoryId() != null) {
			category = categoryRepository.findById(updateEventUserRequest.getCategoryId());
		}
		Event updateEvent = eventRepository
				.save(EventMapper.updateEventDtoToEvent(event, updateEventUserRequest, category));

		return EventMapper.eventToEventFullDto(updateEvent);
	}

	public List<ParticipationRequestDto> getInfoRequest(Long userId, Long eventId) {
		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new ValidationException("Событие не найдено"));
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ValidationException("Пользователь не найден"));
		return requestRepository.findAllByEvent(event).stream()
				.map(RequestMapper::toRequestDto)
				.toList();
	}

	@Transactional
	public EventRequestStatusUpdateResult updateStatusRequest(Long userId, Long eventId,
															  EventRequestStatusUpdateRequest updateRequest) {
		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new NotFoundException("Такого события не найдено."));

		if (!event.getInitiator().getId().equals(userId)) {
			throw new ConflictException("Только владелец может обновить статус запроса");
		}

		if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
			throw new ConflictException("Подтверждение заявок не требуется");
		}

		List<Long> userIds = updateRequest.getRequestIds();
		Status status = updateRequest.getStatus();

		List<Request> requests = requestRepository.findAllById(userIds);

		for (Request request : requests) {
			if (request.getStatus() != Status.PENDING) {
				throw new ConflictException("Статус заявки ID=" + request.getId() +
						" нельзя изменить: текущий статус — " + request.getStatus());
			}
		}

		Map<Long, Request> requestMap = requests.stream()
				.collect(Collectors.toMap(Request::getId, request -> request));

		List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
		List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

		if (status.equals(Status.CONFIRMED)) {
			long confirmed = event.getConfirmedRequests();
			long limit = event.getParticipantLimit();

			if (limit > 0 && confirmed >= limit) {
				throw new ConflictException("Достигнут лимит участников события");
			}

			long availableSlots = (limit == 0) ? requests.size() : limit - confirmed;

			if (availableSlots <= 0) {
				for (Long id : userIds) {
					Request request = requestMap.get(id);
					if (request != null) {
						request.setStatus(Status.REJECTED);
						rejectedRequests.add(RequestMapper.toRequestDto(request));
					}
				}
			} else {
				int confirmedCount = 0;

				for (Long id : userIds) {
					Request request = requestMap.get(id);
					if (request == null) continue;

					if (confirmedCount < availableSlots) {
						request.setStatus(Status.CONFIRMED);
						confirmedRequests.add(RequestMapper.toRequestDto(request));
						confirmedCount++;
					} else {
						request.setStatus(Status.REJECTED);
						rejectedRequests.add(RequestMapper.toRequestDto(request));
					}
				}

				event.setConfirmedRequests(confirmed + confirmedCount);

			}

		} else if (status.equals(Status.REJECTED)) {

			for (Long requestId : userIds) {
				Request request = requestMap.get(requestId);

				if (request != null) {
					request.setStatus(Status.REJECTED);
					rejectedRequests.add(RequestMapper.toRequestDto(request));
				}
			}

		} else {
			throw new ConflictException("Недопустимый статус для обновления: " + status);
		}

		requestRepository.saveAll(requests);

		if (!confirmedRequests.isEmpty()) {
			eventRepository.save(event);
		}

		return EventRequestStatusUpdateResult.builder()
				.confirmedRequests(confirmedRequests)
				.rejectedRequests(rejectedRequests)
				.build();
	}

	public List<ParticipationRequestDto> getInfoOnParticipation(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ValidationException("Пользователь не найден"));
		return requestRepository.findAllByRequester(user).stream()
				.map(RequestMapper::toRequestDto)
				.toList();
	}

	@Transactional
	public ParticipationRequestDto createRequestForParticipation(Long userId, Long eventId) {
		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new NotFoundException("Событие не найдено"));
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("Пользователь не найден"));

		if (event.getInitiator().equals(user)) {
			throw new ConflictException("инициатор события не может добавить запрос на участие в своём событии.");
		}
		if (!event.getState().equals(State.PUBLISHED)) {
			throw new ConflictException("нельзя участвовать в неопубликованном событии.");
		}

		long confirmedRequest = event.getConfirmedRequests();
		if (event.getParticipantLimit() != 0 && event.getParticipantLimit() <= confirmedRequest) {
			throw new ConflictException("у события достигнут лимит запросов на участие");
		}

		Optional<Request> existingRequest = requestRepository.findByEventAndRequester(event, user);
		if (existingRequest.isPresent()) {
			throw new ConflictException("Пользователь уже подал запрос на участие в этом событии.");
		}

		Request request = Request.builder()
				.requester(user)
				.event(event)
				.build();

		if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
			request.setStatus(Status.CONFIRMED);
			event.setConfirmedRequests(confirmedRequest + 1);
			eventRepository.save(event);
		}

		return RequestMapper.toRequestDto(requestRepository.save(request));
	}

	@Transactional
	public ParticipationRequestDto canceledRequestForParticipation(Long userId, Long requestId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("Пользователь не найден"));
		Optional<Request> request = requestRepository.findById(requestId);
		if (request.isPresent()) {
			Request update = request.get();
			update.setStatus(Status.CANCELED);
			requestRepository.save(update);
		} else {
			throw new NotFoundException("Такого запроса нет.");
		}
		return RequestMapper.toRequestDto(request.get());
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

		List<String> uris = events.stream()
				.map(event -> URI_EVENT_ENDPOINT + event.getId())
				.toList();

		Map<String, Long> eventHits = statisticsService.getViewsByUris(uris, false);

		return events.stream()
				.map(event -> {
					Long views = eventHits.getOrDefault(URI_EVENT_ENDPOINT + event.getId(), 0L);
					return EventMapper.toEventFullDto(event, views);
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

		String uri = URI_EVENT_ENDPOINT + event.getId();
		Long views = statisticsService.getViewsByUri(uri,false);

		return EventMapper.toEventFullDto(updatedEvent, views);
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
}
