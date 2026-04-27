package ru.practicum.requestservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.iteractionapi.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.iteractionapi.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.iteractionapi.dto.request.ParticipationRequestDto;
import ru.practicum.iteractionapi.feignapi.requestfeignclient.RequestFeignClient;
import ru.practicum.requestservice.service.RequestService;

import java.util.List;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(path = "/users/{userId}/requests")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestController {
	final RequestService requestService;

	@GetMapping
	public List<ParticipationRequestDto> getInfoOnParticipation(@PathVariable(value = "userId") Long userId) {
		return requestService.getInfoOnParticipation(userId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ParticipationRequestDto createRequestForParticipation(@PathVariable(value = "userId") Long userId,
																 @RequestParam(value = "eventId", required = true) Long eventId,
																 HttpServletRequest request) {
		return requestService.createRequestForParticipation(userId, eventId);
	}

	@PatchMapping("/{requestId}/cancel")
	public ParticipationRequestDto canceledRequestForParticipation(@PathVariable(value = "userId") Long userId,
																   @PathVariable(value = "requestId") Long requestId) {
		return requestService.canceledRequestForParticipation(userId, requestId);
	}

	@GetMapping("/{eventId}/requests")
	public List<ParticipationRequestDto> getInfoRequest(@PathVariable(value = "userId") Long userId,
														@PathVariable(value = "eventId") Long eventId) {
		return requestService.getInfoRequest(userId, eventId);
	}

	@PutMapping("/{eventId}/requests")
	public EventRequestStatusUpdateResult updateStatusRequest(@PathVariable("userId") Long userId,
															  @PathVariable("eventId") Long eventId,
															  @Valid @RequestBody EventRequestStatusUpdateRequest updateRequest) {
		log.info("Received request to update requests statuses: userId={}, eventId={}, requestIds={}, status={}",
				userId, eventId, updateRequest.getRequestIds(), updateRequest.getStatus());
		return requestService.updateStatusRequest(userId, eventId, updateRequest);
	}
}
