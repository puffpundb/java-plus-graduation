package ru.practicum.iteractionapi.feignapi.eventfeignclient.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.iteractionapi.dto.event.EventFullDto;
import ru.practicum.iteractionapi.dto.event.EventShortDto;
import ru.practicum.iteractionapi.dto.event.NewEventDto;
import ru.practicum.iteractionapi.dto.event.UpdateEventUserRequest;

import java.util.List;

@FeignClient(name = "event-service", contextId = "PrivateEventFeignClient")
public interface PrivateEventFeignClient {
	@GetMapping("/users/{userId}/events")
	List<EventShortDto> getEventsByOwner(@PathVariable(value = "userId") Long userId,
										 @RequestParam(value = "from", defaultValue = "0") @PositiveOrZero Long from,
										 @RequestParam(value = "size", defaultValue = "10") @PositiveOrZero Long size);

	@PostMapping("/users/{userId}/events")
	@ResponseStatus(HttpStatus.CREATED)
	EventFullDto createEvent(@PathVariable(value = "userId") Long userId,
							 @Valid @RequestBody NewEventDto newEventDto);

	@GetMapping("/users/{userId}/events/{eventId}")
	EventFullDto getInfoEvent(@PathVariable(value = "userId") Long userId,
							  @PathVariable(value = "eventId") Long eventId);

	@PatchMapping("/users/{userId}/events/{eventId}")
	EventFullDto updateEvent(@PathVariable(value = "userId") Long userId,
							 @PathVariable(value = "eventId") Long eventId,
							 @Valid @RequestBody UpdateEventUserRequest updateEventUserRequest);
}
