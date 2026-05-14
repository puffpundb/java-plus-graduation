package ru.practicum.eventservice.controller.event;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.eventservice.service.EventService;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.iteractionapi.dto.event.EventFullDto;
import ru.practicum.iteractionapi.dto.event.EventSearchParams;
import ru.practicum.iteractionapi.dto.event.EventShortDto;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class PublicEventController {
	final EventService eventService;

	@GetMapping
	public List<EventShortDto> getEvents(@Valid EventSearchParams params,
										 HttpServletRequest request) {
		log.info("PublicEventController: вызов эндпоинта GET events/ " +
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
				request);
	}

	@GetMapping("/{id}")
	public EventFullDto getEventById(@PathVariable(value = "id") Long id,
									 @RequestHeader("X-EWM-USER-ID") long userId) {
		log.info("PublicEventController: вызов эндпоинта GET events/{} от пользователя {}", id, userId);
		return eventService.getById(userId, id);
	}

	@GetMapping("/recommendations")
	public List<RecommendedEventProto> getRecommendations(@RequestHeader("X-EWM-USER-ID") long userId,
														  @RequestParam(defaultValue = "10") int maxResults) {
		log.info("PublicEventController: GET /events/recommendations для userId={}, maxResults={}", userId, maxResults);
		return eventService.getRecommendationsForUser(userId, maxResults);
	}

	@PutMapping("/{eventId}/like")
	public void likeEvent(@PathVariable long eventId,
						  @RequestHeader("X-EWM-USER-ID") long userId) {
		log.info("PublicEventController: PUT /events/{}/like от userId={}", eventId, userId);
		eventService.likeEvent(userId, eventId);
	}
}
