package ru.practicum.eventservice.controller.event;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.eventservice.service.EventService;
import ru.practicum.iteractionapi.dto.event.EventFullDto;
import ru.practicum.iteractionapi.dto.event.EventSearchParams;
import ru.practicum.iteractionapi.dto.event.EventShortDto;
import ru.practicum.iteractionapi.feignapi.eventfeignclient.event.PublicEventFeignClient;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class PublicEventController implements PublicEventFeignClient {
	final EventService eventService;

	@GetMapping
	@Override
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
	@Override
	public EventFullDto getEventById(@PathVariable(value = "id") Long id,
									 HttpServletRequest request) {
		log.info("PublicEventController: вызов эндпоинта GET events/{}", id);

		return eventService.getById(id, request);
	}
}
