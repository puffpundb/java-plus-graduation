package ru.practicum.eventservice.controller.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.eventservice.service.EventService;
import ru.practicum.iteractionapi.dto.event.EventFullDto;
import ru.practicum.iteractionapi.dto.event.EventShortDto;
import ru.practicum.iteractionapi.dto.event.NewEventDto;
import ru.practicum.iteractionapi.dto.event.UpdateEventUserRequest;
import ru.practicum.iteractionapi.feignapi.eventfeignclient.event.PrivateEventFeignClient;

import java.util.List;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(path = "/users/{userId}/events")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrivateEventController implements PrivateEventFeignClient {
	final EventService service;

	@Override
	public List<EventShortDto> getEventsByOwner(@PathVariable(value = "userId") Long userId,
												@RequestParam(value = "from", defaultValue = "0") @PositiveOrZero Long from,
												@RequestParam(value = "size", defaultValue = "10") @PositiveOrZero Long size) {
		return service.getEventsByOwner(userId, from, size);
	}

	@Override
	public EventFullDto createEvent(@PathVariable(value = "userId") Long userId,
									@Valid @RequestBody NewEventDto newEventDto) {
		return service.createEvent(userId, newEventDto);
	}

	@Override
	public EventFullDto getInfoEvent(@PathVariable(value = "userId") Long userId,
									 @PathVariable(value = "eventId") Long eventId) {
		return service.getInfoEvent(userId, eventId);
	}

	@Override
	public EventFullDto updateEvent(@PathVariable(value = "userId") Long userId,
									@PathVariable(value = "eventId") Long eventId,
									@Valid @RequestBody UpdateEventUserRequest updateEventUserRequest) {
		return service.updateEvent(userId, eventId, updateEventUserRequest);
	}
}
