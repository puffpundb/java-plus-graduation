package ru.practicum.eventservice.service;

import io.github.resilience4j.retry.annotation.Retry;
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
import ru.practicum.client.AnalyzerClient;
import ru.practicum.client.CollectorClient;
import ru.practicum.eventservice.entity.Category;
import ru.practicum.eventservice.entity.Event;
import ru.practicum.eventservice.entity.Location;
import ru.practicum.eventservice.mapper.EventMapper;
import ru.practicum.eventservice.repository.CategoryRepository;
import ru.practicum.eventservice.repository.EventRepository;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.iteractionapi.dto.enums.AdminStateAction;
import ru.practicum.iteractionapi.dto.event.*;
import ru.practicum.iteractionapi.dto.user.UserDto;
import ru.practicum.iteractionapi.error.ConflictException;
import ru.practicum.iteractionapi.error.NotFoundException;
import ru.practicum.iteractionapi.error.ValidationException;
import ru.practicum.iteractionapi.feignapi.requestfeignclient.InternalRequestsFeignClient;
import ru.practicum.iteractionapi.feignapi.requestfeignclient.RequestFeignClient;
import ru.practicum.iteractionapi.feignapi.userfeignclient.UserFeignClient;
import ru.practicum.iteractionapi.model.enums.State;

import java.time.LocalDateTime;
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

	final CollectorClient collectorClient;
	final AnalyzerClient analyzerClient;

	final UserFeignClient userFeignClient;
	final RequestFeignClient requestFeignClient;
	final InternalRequestsFeignClient internalRequestsFeignClient;

	static final String URI_EVENT_ENDPOINT = "/events/";

	@Retry(name = "eventServiceRetry", fallbackMethod = "fallbackFindUsers")
	protected List<UserDto> findUsers(List<Long> ids, int from, int size) {
		return userFeignClient.findUsers(ids, from, size);
	}

	private List<UserDto> fallbackFindUsers(List<Long> ids, int from, int size, Throwable t) {
		log.warn("EventService: User-service недоступен, возвращаем пустой список. Причина: {}", t.getMessage());
		return Collections.emptyList();
	}

	@Retry(name = "eventServiceRetry", fallbackMethod = "fallbackGetConfirmedRequestsCounts")
	protected Map<Long, Long> getConfirmedRequestsCounts(List<Long> eventIds) {
		return internalRequestsFeignClient.getConfirmedRequestsCounts(eventIds);
	}

	private Map<Long, Long> fallbackGetConfirmedRequestsCounts(List<Long> eventIds, Throwable t) {
		log.warn("EventService: Request-service недоступен, возвращаем 0 для eventIds: {}. Причина: {}", eventIds, t.getMessage());
		if (eventIds == null || eventIds.isEmpty()) return Map.of();
		return eventIds.stream().collect(Collectors.toMap(id -> id, id -> 0L));
	}

	@Retry(name = "eventServiceRetry", fallbackMethod = "fallbackGetConfirmedRequestsCount")
	protected Long getConfirmedRequestsCount(Long eventId) {
		return internalRequestsFeignClient.getConfirmedRequestsCount(eventId);
	}

	private Long fallbackGetConfirmedRequestsCount(Long eventId, Throwable t) {
		log.warn("EventService: Request-service недоступен, возвращаем 0 для eventId: {}. Причина: {}", eventId, t.getMessage());
		return 0L;
	}

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

		log.info("PublicEventService: Поиск ивентов с заданными параметрами: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, onlyAvailable={}, sort={}, from={}, size={}",
				text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

		Pageable pageable = PageRequest.of(from / size, size);
		List<Event> eventsList = eventRepository.findPublicEvents(text, categories, paid, rangeStart, rangeEnd, pageable);

		if (eventsList.isEmpty()) {
			log.info("PublicEventService: Ивенты не найдены, возвращаем пустой список");
			return Collections.emptyList();
		}

		List<Long> initiatorIds = eventsList.stream()
				.map(Event::getInitiatorId)
				.distinct()
				.toList();

		List<UserDto> users = findUsers(initiatorIds, 0, initiatorIds.size());

		if (users == null || users.isEmpty()) {
			log.error("Не удалось получить данные пользователей для событий. Возвращаем пустой результат.");
			return Collections.emptyList();
		}

		Map<Long, UserDto> userMap = users.stream()
				.collect(Collectors.toMap(UserDto::getId, Function.identity()));

		List<Long> eventIds = eventsList.stream().map(Event::getId).toList();
		log.debug("Получение рейтингов для eventIds={}", eventIds);
		Map<Long, Double> ratingMap = analyzerClient.getInteractionsCount(eventIds)
				.collect(Collectors.toMap(RecommendedEventProto::getEventId, RecommendedEventProto::getScore));
		log.debug("Получены рейтинги: {}", ratingMap);

		Map<Long, Long> confirmedMap = getConfirmedRequestsCounts(eventIds);
		log.debug("Получены подтверждённые заявки: {}", confirmedMap);

		if (onlyAvailable) {
			eventsList = eventsList.stream()
					.filter(event -> event.getParticipantLimit() == 0 ||
							confirmedMap.getOrDefault(event.getId(), 0L) < event.getParticipantLimit())
					.toList();
			if (eventsList.isEmpty()) {
				log.info("После фильтрации onlyAvailable события не найдены");
				return Collections.emptyList();
			}
		}

		List<EventShortDto> result = eventsList.stream()
				.map(event -> {
					UserDto initiator = userMap.get(event.getInitiatorId());
					Long confirmed = confirmedMap.getOrDefault(event.getId(), 0L);
					Double rating = ratingMap.getOrDefault(event.getId(), 0.0);
					return EventMapper.toEventShortDto(event, initiator, confirmed, rating);
				})
				.toList();

		if (sort == EventSort.VIEWS) {
			log.debug("Сортировка по рейтингу (было VIEWS)");
			result = result.stream()
					.sorted(Comparator.comparingDouble(EventShortDto::getRating).reversed())
					.toList();
		}

		log.info("Найдено {} событий", result.size());
		return result;
	}

	public EventFullDto internalGetById(Long id) {
		log.info("PublicEventService: Поиск ивента с id={} (внутренний вызов)", id);
		Event event = eventRepository.findById(id)
				.orElseThrow(() -> new NotFoundException(String.format("Событие с id: %d не найдено", id)));

		List<UserDto> userList = findUsers(List.of(event.getInitiatorId()), 0, 1);
		if (userList.isEmpty()) throw new NotFoundException("Пользователь не найдены: " + event.getInitiatorId());
		UserDto userDto = userList.getFirst();

		Long confirmed = getConfirmedRequestsCount(id);

		Double rating = analyzerClient.getRatingForEvent(id);
		log.debug("Рейтинг события {} = {}", id, rating);

		return EventMapper.toEventFullDto(event, userDto, confirmed, rating);
	}

	public EventFullDto getById(long userId, long eventId) {
		log.info("PublicEventService: Поиск опубликованного ивента с id={} для пользователя {}", eventId, userId);
		Event event = eventRepository.findPublishedById(eventId)
				.orElseThrow(() -> new NotFoundException(String.format("Опубликованное событие с id: %d не найдено", eventId)));

		collectorClient.sendView(userId, eventId);
		log.debug("Отправлен VIEW в Collector: userId={}, eventId={}", userId, eventId);

		Double rating = analyzerClient.getRatingForEvent(eventId);
		log.debug("Рейтинг события {} = {}", eventId, rating);

		List<UserDto> userList = findUsers(List.of(event.getInitiatorId()), 0, 1);
		if (userList.isEmpty()) throw new NotFoundException("Пользователь не найден: " + event.getInitiatorId());
		UserDto initiator = userList.getFirst();

		Long confirmed = getConfirmedRequestsCount(eventId);

		return EventMapper.toEventFullDto(event, initiator, confirmed, rating);
	}

	public List<EventShortDto> getEventsByOwner(Long userId, Long from, Long size) {
		log.info("EventService: Получение событий пользователя userId={}, from={}, size={}", userId, from, size);
		int page = from.intValue() / size.intValue();
		Pageable pageable = PageRequest.of(page, size.intValue());

		Page<Event> eventPage = eventRepository.findByInitiatorId(userId, pageable);
		List<Event> events = eventPage.getContent();

		if (events.isEmpty()) {
			log.info("У пользователя {} нет событий", userId);
			return Collections.emptyList();
		}

		List<UserDto> userList = findUsers(List.of(userId), 0, 1);
		if (userList.isEmpty()) throw new NotFoundException("Пользователь не найден: " + userId);
		UserDto initiator = userList.getFirst();

		List<Long> eventIds = events.stream().map(Event::getId).toList();
		Map<Long, Long> confirmedMap = getConfirmedRequestsCounts(eventIds);
		Map<Long, Double> ratingMap = analyzerClient.getInteractionsCount(eventIds)
				.collect(Collectors.toMap(RecommendedEventProto::getEventId, RecommendedEventProto::getScore));

		List<EventShortDto> dtos = events.stream()
				.map(event -> {
					Long confirmed = confirmedMap.getOrDefault(event.getId(), 0L);
					Double rating = ratingMap.getOrDefault(event.getId(), 0.0);
					return EventMapper.toEventShortDto(event, initiator, confirmed, rating);
				})
				.collect(Collectors.toList());

		log.info("Найдено {} событий для пользователя {}", dtos.size(), userId);
		return dtos;
	}

	@Transactional
	public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
		log.info("EventService: Создание события пользователем userId={}, newEventDto={}", userId, newEventDto);
		if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
			throw new ValidationException("Время события должно быть за два часа до события.");
		}

		Category category = categoryRepository.findById(newEventDto.getCategoryId())
				.orElseThrow(() -> new ValidationException("Категория не указана"));

		List<UserDto> userList = findUsers(List.of(userId), 0, 1);
		if (userList.isEmpty()) throw new NotFoundException("Такого пользователя не существует.");

		Event event = eventRepository.save(EventMapper.newEventDtoToEvent(newEventDto, userId, category));

		Long confirmed = 0L;
		Double rating = 0.0;
		log.info("Событие создано: id={}", event.getId());
		return EventMapper.toEventFullDto(event, userList.getFirst(), confirmed, rating);
	}

	public EventFullDto getInfoEvent(Long userId, Long eventId) {
		log.info("EventService: Получение информации о событии eventId={} для userId={}", eventId, userId);
		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new NotFoundException("Такого события не найдено."));

		List<UserDto> userList = findUsers(List.of(userId), 0, 1);
		if (userList.isEmpty()) throw new NotFoundException("Такого пользователя не существует.");

		Long confirmed = getConfirmedRequestsCount(eventId);
		Double rating = analyzerClient.getRatingForEvent(eventId);
		log.debug("Рейтинг события {} = {}", eventId, rating);

		return EventMapper.toEventFullDto(event, userList.getFirst(), confirmed, rating);
	}

	@Transactional
	public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
		log.info("EventService: Обновление события userId={}, eventId={}, updateRequest={}", userId, eventId, updateEventUserRequest);
		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new NotFoundException("Такого события не найдено."));

		List<UserDto> userList = findUsers(List.of(userId), 0, 1);
		if (userList.isEmpty()) throw new NotFoundException("Такого пользователя не существует.");

		if (event.getState().equals(State.PUBLISHED)) {
			throw new ConflictException("Данное событие нельзя изменить.");
		}
		Optional<Category> category = Optional.empty();
		if (updateEventUserRequest.getCategoryId() != null) {
			category = categoryRepository.findById(updateEventUserRequest.getCategoryId());
		}
		Event updateEvent = eventRepository.save(EventMapper.updateEventDtoToEvent(event, updateEventUserRequest, category));

		Long confirmed = getConfirmedRequestsCount(updateEvent.getId());
		Double rating = analyzerClient.getRatingForEvent(updateEvent.getId());

		log.info("Событие {} обновлено", eventId);
		return EventMapper.toEventFullDto(updateEvent, userList.getFirst(), confirmed, rating);
	}

	public List<EventFullDto> getFullEvents(AdminEventParam params) {
		log.info("AdminEventService: Получение событий с параметрами: {}", params);
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

		if (events.isEmpty()) {
			log.info("События не найдены");
			return Collections.emptyList();
		}

		List<Long> initiatorIds = events.stream()
				.map(Event::getInitiatorId)
				.distinct()
				.toList();

		List<UserDto> users = findUsers(initiatorIds, 0, initiatorIds.size());
		if (users.isEmpty()) {
			log.error("Не удалось получить данные пользователей для событий. Возвращаем пустой список.");
			return Collections.emptyList();
		}
		Map<Long, UserDto> userMap = users.stream()
				.collect(Collectors.toMap(UserDto::getId, Function.identity()));

		List<Long> eventIds = events.stream().map(Event::getId).toList();
		Map<Long, Long> confirmedMap = getConfirmedRequestsCounts(eventIds);
		Map<Long, Double> ratingMap = analyzerClient.getInteractionsCount(eventIds)
				.collect(Collectors.toMap(RecommendedEventProto::getEventId, RecommendedEventProto::getScore));

		List<EventFullDto> result = events.stream()
				.map(event -> {
					UserDto initiator = userMap.get(event.getInitiatorId());
					if (initiator == null) {
						throw new NotFoundException("User not found: " + event.getInitiatorId());
					}
					Long confirmed = confirmedMap.getOrDefault(event.getId(), 0L);
					Double rating = ratingMap.getOrDefault(event.getId(), 0.0);
					return EventMapper.toEventFullDto(event, initiator, confirmed, rating);
				})
				.toList();

		log.info("Найдено {} событий", result.size());
		return result;
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
		log.info("AdminEventService: Обновление события eventId={} запросом: {}", eventId, request);
		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено."));

		if (request.getStateAction() != null) {
			if (request.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
				if (event.getState() != State.PENDING) {
					throw new ConflictException("Cобытие можно публиковать, только если оно в состоянии ожидания публикации");
				}
				event.setState(State.PUBLISHED);
				event.setPublishedOn(LocalDateTime.now());
				log.info("Событие {} опубликовано", eventId);
			} else if (request.getStateAction() == AdminStateAction.REJECT_EVENT) {
				if (event.getState() == State.PUBLISHED) {
					throw new ConflictException("Cобытие можно отклонить, только если оно еще не опубликовано");
				}
				event.setState(State.CANCELED);
				log.info("Событие {} отклонено", eventId);
			}
		}

		updateEventFields(event, request);
		Event updatedEvent = eventRepository.save(event);

		List<UserDto> users = findUsers(List.of(updatedEvent.getInitiatorId()), 0, 1);
		if (users.isEmpty()) throw new NotFoundException("Пользователь не найден: " + updatedEvent.getInitiatorId());
		UserDto initiator = users.getFirst();

		Long confirmed = getConfirmedRequestsCount(updatedEvent.getId());
		Double rating = analyzerClient.getRatingForEvent(updatedEvent.getId());

		return EventMapper.toEventFullDto(updatedEvent, initiator, confirmed, rating);
	}

	private void updateEventFields(Event event, UpdateEventAdminRequest request) {
		if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
		if (request.getCategoryId() != null) {
			Category category = categoryRepository.findById(request.getCategoryId())
					.orElseThrow(() -> new NotFoundException("Категория с id=" + request.getCategoryId() + " не найдена."));
			event.setCategory(category);
		}
		if (request.getDescription() != null) event.setDescription(request.getDescription());
		if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
		if (request.getLocationDto() != null) {
			event.setLocation(Location.builder().lat(request.getLocationDto().getLat()).lon(request.getLocationDto().getLon()).build());
		}
		if (request.getPaid() != null) event.setPaid(request.getPaid());
		if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
		if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
		if (request.getTitle() != null) event.setTitle(request.getTitle());
	}

	public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
		log.info("Запрос рекомендаций для пользователя {} с лимитом {}", userId, maxResults);
		List<RecommendedEventProto> recommendations = analyzerClient.getRecommendationsForUser(userId, maxResults)
				.collect(Collectors.toList());
		log.debug("Получено {} рекомендаций", recommendations.size());
		return recommendations;
	}

	public void likeEvent(long userId, long eventId) {
		log.info("Пользователь {} ставит лайк событию {}", userId, eventId);
		collectorClient.sendLike(userId, eventId);
		log.debug("Лайк отправлен в Collector");
	}
}
