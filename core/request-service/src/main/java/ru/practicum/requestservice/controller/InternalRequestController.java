package ru.practicum.requestservice.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.requestservice.service.RequestService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/internal/events")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InternalRequestController {
	private final RequestService service;

	@GetMapping("/{eventId}/confirmed-count")
	public Long getConfirmedRequestsCount(@PathVariable Long eventId) {
		return service.getConfirmedRequestsCount(eventId);
	}

	@GetMapping("/confirmed-count")
	public Map<Long, Long> getConfirmedRequestsCounts(@RequestParam("eventIds") List<Long> eventIds) {
		return service.getConfirmedRequestsCounts(eventIds);
	}
}
