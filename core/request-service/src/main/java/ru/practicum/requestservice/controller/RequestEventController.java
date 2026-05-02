package ru.practicum.requestservice.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.iteractionapi.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.iteractionapi.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.iteractionapi.dto.request.ParticipationRequestDto;
import ru.practicum.requestservice.service.RequestService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestEventController {
	private final RequestService requestService;

	@GetMapping("/{eventId}/requests")
	public List<ParticipationRequestDto> getInfoRequest(@PathVariable Long userId,
														@PathVariable Long eventId) {
		return requestService.getInfoRequest(userId, eventId);
	}

	@PatchMapping("/{eventId}/requests")
	public EventRequestStatusUpdateResult updateStatusRequest(@PathVariable Long userId,
															  @PathVariable Long eventId,
															  @Valid @RequestBody EventRequestStatusUpdateRequest updateRequest) {
		log.info("Received request to update requests statuses: userId={}, eventId={}, requestIds={}, status={}",
				userId, eventId, updateRequest.getRequestIds(), updateRequest.getStatus());
		return requestService.updateStatusRequest(userId, eventId, updateRequest);
	}
}
