package ru.practicum.requestservice.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.iteractionapi.dto.event.EventFullDto;
import ru.practicum.iteractionapi.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.iteractionapi.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.iteractionapi.dto.request.ParticipationRequestDto;
import ru.practicum.iteractionapi.dto.user.UserDto;
import ru.practicum.iteractionapi.error.ConflictException;
import ru.practicum.iteractionapi.error.NotFoundException;
import ru.practicum.iteractionapi.feignapi.eventfeignclient.event.InternalEventFeignClient;
import ru.practicum.iteractionapi.feignapi.eventfeignclient.event.PublicEventFeignClient;
import ru.practicum.iteractionapi.feignapi.userfeignclient.UserFeignClient;
import ru.practicum.iteractionapi.model.enums.State;
import ru.practicum.iteractionapi.model.enums.Status;
import ru.practicum.requestservice.entity.Request;
import ru.practicum.requestservice.mapper.RequestMapper;
import ru.practicum.requestservice.repository.RequestRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class RequestService {
	final RequestRepository requestRepository;

	final PublicEventFeignClient publicEventFeignClient;
	final InternalEventFeignClient internalEventFeignClient;
	final UserFeignClient userFeignClient;

	private EventFullDto checkAndGetEvent(Long eventId) {
		return publicEventFeignClient.getEventById(eventId, null);
	}

	private void checkUser(Long userId) {
		List<UserDto> user = userFeignClient.findUsers(List.of(userId), 0, 1);
		if (user.isEmpty()) throw new NotFoundException("Пользователь не найден");
	}

	public List<ParticipationRequestDto> getInfoRequest(Long userId, Long eventId) {
		checkAndGetEvent(eventId);
		checkUser(userId);

		return requestRepository.findAllByEventId(eventId).stream()
				.map(RequestMapper::toRequestDto)
				.toList();
	}

	@Transactional
	public EventRequestStatusUpdateResult updateStatusRequest(Long userId, Long eventId,
															  EventRequestStatusUpdateRequest updateRequest) {
		EventFullDto event = checkAndGetEvent(eventId);

		if (!event.getInitiatorDto().getId().equals(userId)) {
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

		long confirmedCount = 0;

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
			long newConfirmed = event.getConfirmedRequests() + confirmedCount;
			internalEventFeignClient.setConfirmedRequests(eventId, newConfirmed);
		}

		return EventRequestStatusUpdateResult.builder()
				.confirmedRequests(confirmedRequests)
				.rejectedRequests(rejectedRequests)
				.build();
	}

	public List<ParticipationRequestDto> getInfoOnParticipation(Long userId) {
		checkUser(userId);

		return requestRepository.findAllByRequesterId(userId).stream()
				.map(RequestMapper::toRequestDto)
				.toList();
	}

	@Transactional
	public ParticipationRequestDto createRequestForParticipation(Long userId, Long eventId) {
		EventFullDto event = checkAndGetEvent(eventId);
		checkUser(userId);

		if (event.getInitiatorDto().getId().equals(userId)) {
			throw new ConflictException("инициатор события не может добавить запрос на участие в своём событии.");
		}
		if (!event.getState().equals(State.PUBLISHED)) {
			throw new ConflictException("нельзя участвовать в неопубликованном событии.");
		}

		long confirmedRequest = event.getConfirmedRequests();
		if (event.getParticipantLimit() != 0 && event.getParticipantLimit() <= confirmedRequest) {
			throw new ConflictException("у события достигнут лимит запросов на участие");
		}

		Optional<Request> existingRequest = requestRepository.findByEventIdAndRequesterId(eventId, userId);
		if (existingRequest.isPresent()) {
			throw new ConflictException("Пользователь уже подал запрос на участие в этом событии.");
		}

		Request request = Request.builder()
				.requester(userId)
				.event(eventId)
				.build();

		if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
			request.setStatus(Status.CONFIRMED);

			internalEventFeignClient.setConfirmedRequests(eventId, event.getConfirmedRequests() + 1);
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
}
