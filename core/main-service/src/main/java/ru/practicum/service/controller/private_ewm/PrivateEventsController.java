package ru.practicum.service.controller.private_ewm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.service.dto.*;
import ru.practicum.service.service.EventService;

import java.util.List;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(path = "/users/{userId}/events")
public class PrivateEventsController {
    private final EventService service;

    @GetMapping
    public List<EventShortDto> getEventsByOwner(
            @PathVariable(value = "userId") Long userId,
            @RequestParam(value = "from", defaultValue = "0") @PositiveOrZero Long from,
            @RequestParam(value = "size", defaultValue = "10") @PositiveOrZero Long size) {
        return service.getEventsByOwner(userId, from, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@PathVariable(value = "userId") Long userId,
                                    @Valid @RequestBody NewEventDto newEventDto) {
        return service.createEvent(userId, newEventDto);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getInfoEvent(@PathVariable(value = "userId") Long userId,
                                     @PathVariable(value = "eventId") Long eventId) {
        return service.getInfoEvent(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(@PathVariable(value = "userId") Long userId,
                                    @PathVariable(value = "eventId") Long eventId,
                                    @Valid @RequestBody UpdateEventUserRequest updateEventUserRequest) {
        return service.updateEvent(userId, eventId, updateEventUserRequest);
    }

    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getInfoRequest(@PathVariable(value = "userId") Long userId,
                                                        @PathVariable(value = "eventId") Long eventId) {
        return service.getInfoRequest(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult updateStatusRequest(
            @PathVariable(value = "userId") Long userId,
            @PathVariable(value = "eventId") Long eventId,
            @Valid @RequestBody EventRequestStatusUpdateRequest updateRequest) {
        return service.updateStatusRequest(userId, eventId, updateRequest);
    }

}
