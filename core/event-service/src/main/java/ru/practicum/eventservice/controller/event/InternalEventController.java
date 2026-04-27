package ru.practicum.eventservice.controller.event;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.eventservice.service.EventService;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InternalEventController {
	final EventService eventService;

	@PatchMapping("/{eventId}/confirmed-requests")
	@ResponseStatus(HttpStatus.OK)
	public void setConfirmedRequests(@PathVariable Long eventId,
									 @RequestParam Long confirmedRequests) {
		log.info("Internal: Фиксация нового значения подтвержденных запросов event - {}, confirmedRequests - {}", eventId, confirmedRequests);
		eventService.setConfirmedRequests(eventId, confirmedRequests);
	}
}
