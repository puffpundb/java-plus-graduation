package ru.practicum.requestservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
public class RequestController implements RequestFeignClient {
	final RequestService requestService;

	@GetMapping
	@Override
	public List<ParticipationRequestDto> getInfoOnParticipation(@PathVariable(value = "userId") Long userId) {
		return requestService.getInfoOnParticipation(userId);
	}

	@PostMapping
	@Override
	public ParticipationRequestDto createRequestForParticipation(@PathVariable(value = "userId") Long userId,
																 @RequestParam(value = "eventId", required = true) Long eventId,
																 HttpServletRequest request) {
		return requestService.createRequestForParticipation(userId, eventId);
	}

	@PatchMapping("/{requestId}/cancel")
	@Override
	public ParticipationRequestDto canceledRequestForParticipation(@PathVariable(value = "userId") Long userId,
																   @PathVariable(value = "requestId") Long requestId) {
		return requestService.canceledRequestForParticipation(userId, requestId);
	}

	@Override
	public List<ParticipationRequestDto> getInfoRequest(@PathVariable(value = "userId") Long userId,
														@PathVariable(value = "eventId") Long eventId) {
		return requestService.getInfoRequest(userId, eventId);
	}

	@Override
	public EventRequestStatusUpdateResult updateStatusRequest(@PathVariable(value = "userId") Long userId,
															  @PathVariable(value = "eventId") Long eventId,
															  @Valid @RequestBody EventRequestStatusUpdateRequest updateRequest) {
		return requestService.updateStatusRequest(userId, eventId, updateRequest);
	}
}
