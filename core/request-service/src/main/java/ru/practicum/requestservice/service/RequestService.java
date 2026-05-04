package ru.practicum.requestservice.service;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.iteractionapi.dto.event.EventFullDto;
import ru.practicum.iteractionapi.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.iteractionapi.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.requestservice.entity.EventConfirmedCount;
import ru.practicum.iteractionapi.dto.request.ParticipationRequestDto;
import ru.practicum.iteractionapi.dto.user.UserDto;
import ru.practicum.iteractionapi.error.ConflictException;
import ru.practicum.iteractionapi.error.EventNotPublishedException;
import ru.practicum.iteractionapi.error.NotFoundException;
import ru.practicum.iteractionapi.feignapi.eventfeignclient.event.InternalEventFeignClient;
import ru.practicum.iteractionapi.feignapi.userfeignclient.UserFeignClient;
import ru.practicum.iteractionapi.model.enums.State;
import ru.practicum.iteractionapi.model.enums.Status;
import ru.practicum.requestservice.entity.Request;
import ru.practicum.requestservice.mapper.RequestMapper;
import ru.practicum.requestservice.repository.RequestRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class RequestService {
	final RequestRepository requestRepository;

	final InternalEventFeignClient internalEventFeignClient;
	final UserFeignClient userFeignClient;

	@Retry(name = "requestServiceRetry", fallbackMethod = "fallbackGetEventById")
	protected EventFullDto getEventById(Long eventId) {
		return internalEventFeignClient.getEventById(eventId);
	}

	private EventFullDto fallbackGetEventById(Long eventId, Throwable t) {
		log.error("Event-service недоступен, невозможно получить событие {}", eventId, t);
		throw new RuntimeException("Сервис событий временно недоступен. Попробуйте позже.");
	}

	@Retry(name = "requestServiceRetry", fallbackMethod = "fallbackFindUsers")
	protected List<UserDto> findUsers(List<Long> ids, int from, int size) {
		return userFeignClient.findUsers(ids, from, size);
	}

	private List<UserDto> fallbackFindUsers(List<Long> ids, int from, int size, Throwable t) {
		log.warn("User-service недоступен, возвращаем пустой список пользователей. Причина: {}", t.getMessage());
		return Collections.emptyList();
	}

	@Transactional
	public EventRequestStatusUpdateResult updateStatusRequest(Long userId, Long eventId,
															  EventRequestStatusUpdateRequest updateRequest) {
		EventFullDto event = getEventById(eventId);

		if (!event.getInitiatorDto().getId().equals(userId)) {
			throw new ConflictException("Только владелец может обновить статус запроса");
		}
		if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
			throw new ConflictException("Подтверждение заявок не требуется");
		}

		List<Long> requestIds = updateRequest.getRequestIds();
		Status newStatus = updateRequest.getStatus();

		List<Request> requests = requestRepository.findAllById(requestIds);
		if (requests.size() != requestIds.size()) {
			throw new NotFoundException("Некоторые заявки не найдены");
		}

		for (Request req : requests) {
			if (req.getStatus() != Status.PENDING) {
				throw new ConflictException("Статус заявки ID=" + req.getId() +
						" нельзя изменить: текущий статус — " + req.getStatus());
			}
		}

		Map<Long, Request> requestMap = requests.stream()
				.collect(Collectors.toMap(Request::getId, r -> r));

		List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
		List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

		if (newStatus == Status.CONFIRMED) {
			long alreadyConfirmed = requestRepository.countByEventIdAndStatus(eventId, Status.CONFIRMED);
			long limit = event.getParticipantLimit();

			if (limit > 0 && alreadyConfirmed >= limit) {
				throw new ConflictException("Достигнут лимит участников события");
			}

			long availableSlots = (limit == 0) ? requests.size() : limit - alreadyConfirmed;
			int confirmedCount = 0;

			for (Long id : requestIds) {
				Request req = requestMap.get(id);
				if (req == null) continue;
				if (confirmedCount < availableSlots) {
					req.setStatus(Status.CONFIRMED);
					confirmedRequests.add(RequestMapper.toRequestDto(req));
					confirmedCount++;
				} else {
					req.setStatus(Status.REJECTED);
					rejectedRequests.add(RequestMapper.toRequestDto(req));
				}
			}

			requestRepository.saveAll(requests);

		} else if (newStatus == Status.REJECTED) {
			for (Request req : requests) {
				req.setStatus(Status.REJECTED);
				rejectedRequests.add(RequestMapper.toRequestDto(req));
			}
			requestRepository.saveAll(requests);
		} else {
			throw new ConflictException("Недопустимый статус для обновления: " + newStatus);
		}

		return EventRequestStatusUpdateResult.builder()
				.confirmedRequests(confirmedRequests)
				.rejectedRequests(rejectedRequests)
				.build();
	}

	private EventFullDto checkAndGetEvent(Long eventId) {
		return getEventById(eventId);
	}

	private void checkUser(Long userId) {
		List<UserDto> user = findUsers(List.of(userId), 0, 1);
		if (user == null || user.isEmpty()) {
			throw new NotFoundException("Пользователь не найден или сервис пользователей недоступен");
		}
	}

	public List<ParticipationRequestDto> getInfoRequest(Long userId, Long eventId) {
		checkAndGetEvent(eventId);
		checkUser(userId);
		return requestRepository.findAllByEventId(eventId).stream()
				.map(RequestMapper::toRequestDto)
				.toList();
	}

	@Transactional
	public ParticipationRequestDto createRequestForParticipation(Long userId, Long eventId) {
		EventFullDto event = checkAndGetEvent(eventId);
		checkUser(userId);

		if (!event.getState().equals(State.PUBLISHED)) {
			throw new EventNotPublishedException("Событие с id: " + eventId + " не опубликовано");
		}
		if (event.getInitiatorDto().getId().equals(userId)) {
			throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии.");
		}

		long confirmedRequest = requestRepository.countByEventIdAndStatus(eventId, Status.CONFIRMED);

		if (event.getParticipantLimit() != 0 && event.getParticipantLimit() <= confirmedRequest) {
			throw new ConflictException("У события достигнут лимит запросов на участие");
		}

		Optional<Request> existingRequest = requestRepository.findByEventIdAndRequesterId(eventId, userId);
		if (existingRequest.isPresent()) {
			throw new ConflictException("Пользователь уже подал запрос на участие в этом событии.");
		}

		Request request = Request.builder()
				.requesterId(userId)
				.eventId(eventId)
				.build();

		if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
			request.setStatus(Status.CONFIRMED);
		}

		return RequestMapper.toRequestDto(requestRepository.save(request));
	}

	@Transactional
	public ParticipationRequestDto canceledRequestForParticipation(Long userId, Long requestId) {
		checkUser(userId);

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

	public List<ParticipationRequestDto> getInfoOnParticipation(Long userId) {
		checkUser(userId);
		return requestRepository.findAllByRequesterId(userId).stream()
				.map(RequestMapper::toRequestDto)
				.toList();
	}

	public Long getConfirmedRequestsCount(Long eventId) {
		return requestRepository.countByEventIdAndStatus(eventId, Status.CONFIRMED);
	}

	public Map<Long, Long> getConfirmedRequestsCounts(List<Long> eventIds) {
		return requestRepository.countByEventIdInAndStatus(eventIds, Status.CONFIRMED)
				.stream()
				.collect(Collectors.toMap(EventConfirmedCount::getEventId, EventConfirmedCount::getCount));
	}
}
