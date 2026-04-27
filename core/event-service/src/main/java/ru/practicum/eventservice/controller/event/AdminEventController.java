package ru.practicum.eventservice.controller.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.eventservice.service.EventService;
import ru.practicum.iteractionapi.dto.event.AdminEventParam;
import ru.practicum.iteractionapi.dto.event.EventFullDto;
import ru.practicum.iteractionapi.dto.event.UpdateEventAdminRequest;
import ru.practicum.iteractionapi.feignapi.eventfeignclient.event.AdminEventFeignClient;

import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Validated
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminEventController implements AdminEventFeignClient {
	final EventService service;

	@Override
	public List<EventFullDto> getEvents(@Valid @ModelAttribute AdminEventParam params) {
		log.info("GET /admin/events c параметрами: " +
						"users={}, states={}, categories={}, rangeStart={}, rangeEnd={}, from={}, size={}",
				params.getUsers(), params.getStates(), params.getCategories(), params.getRangeStart(),
				params.getRangeEnd(), params.getFrom(), params.getSize());

		return service.getFullEvents(params);
	}

	@Override
	public EventFullDto updateEvents(@PathVariable(name = "eventId") @Positive Long eventId,
									 @RequestBody @Valid UpdateEventAdminRequest dto) {
		log.info("PATCH /admin/events/{} с телом: {}", eventId, dto);

		return service.updateEventByAdmin(eventId, dto);
	}
}
