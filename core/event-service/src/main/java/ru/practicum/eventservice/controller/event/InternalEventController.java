package ru.practicum.eventservice.controller.event;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.eventservice.service.EventService;
import ru.practicum.iteractionapi.dto.event.EventFullDto;
import ru.practicum.iteractionapi.dto.event.EventSearchParams;
import ru.practicum.iteractionapi.dto.event.EventShortDto;

import java.util.List;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InternalEventController {
	final EventService eventService;

	@PutMapping("/{eventId}/confirmed-requests")
	@ResponseStatus(HttpStatus.OK)
	public void setConfirmedRequests(@PathVariable Long eventId,
									 @RequestParam Long confirmedRequests) {
		log.info("Internal: Фиксация нового значения подтвержденных запросов event - {}, confirmedRequests - {}", eventId, confirmedRequests);
		eventService.setConfirmedRequests(eventId, confirmedRequests);
	}

	@GetMapping("/events")
	public List<EventShortDto> getEvents(@SpringQueryMap @Valid EventSearchParams params) {
		log.info("InternalEventController: вызов эндпоинта GET events/ " +
						"с параметрами запроса --  " +
						"text:{}, categories:{}, paid:{}, rangeStart:{}, rangeEnd:{}, onlyAvailable:{}, sort:{}, from:{}, size:{}",
				params.getText(), params.getCategories(), params.getPaid(), params.getRangeStart(), params.getRangeEnd(),
				params.getOnlyAvailable(), params.getSort(), params.getFrom(), params.getSize());

		return eventService.getEvents(params.getText(),
				params.getCategories(),
				params.getPaid(),
				params.getRangeStart(),
				params.getRangeEnd(),
				params.getOnlyAvailable(),
				params.getSort(),
				params.getFrom(),
				params.getSize(),
				null);
	}

	@GetMapping("/events/{id}")
	public EventFullDto getEventById(@PathVariable(value = "id") Long id) {
		log.info("InternalEventController: вызов эндпоинта GET events/{}", id);

		return eventService.getById(id, null);
	}
}
