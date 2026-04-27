package ru.practicum.iteractionapi.feignapi.requestfeignclient;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.iteractionapi.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.iteractionapi.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.iteractionapi.dto.request.ParticipationRequestDto;

import java.util.List;

@FeignClient(name = "request-service")
@RequestMapping(path = "/users/{userId}/requests")
public interface RequestFeignClient {
	@GetMapping
	List<ParticipationRequestDto> getInfoOnParticipation(@PathVariable(value = "userId") Long userId);

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ParticipationRequestDto createRequestForParticipation(@PathVariable(value = "userId") Long userId,
														  @RequestParam(value = "eventId", required = true) Long eventId,
														  HttpServletRequest request);

	@PatchMapping("/{requestId}/cancel")
	public ParticipationRequestDto canceledRequestForParticipation(@PathVariable(value = "userId") Long userId,
																   @PathVariable(value = "requestId") Long requestId);

	@GetMapping("/{eventId}/requests")
	List<ParticipationRequestDto> getInfoRequest(@PathVariable(value = "userId") Long userId,
												 @PathVariable(value = "eventId") Long eventId);

	@PatchMapping("/{eventId}/requests")
	EventRequestStatusUpdateResult updateStatusRequest(@PathVariable(value = "userId") Long userId,
													   @PathVariable(value = "eventId") Long eventId,
													   @Valid @RequestBody EventRequestStatusUpdateRequest updateRequest);
}
