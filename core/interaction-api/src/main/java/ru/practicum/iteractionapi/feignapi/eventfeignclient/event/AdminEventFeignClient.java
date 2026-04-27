package ru.practicum.iteractionapi.feignapi.eventfeignclient.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.iteractionapi.dto.event.AdminEventParam;
import ru.practicum.iteractionapi.dto.event.EventFullDto;
import ru.practicum.iteractionapi.dto.event.UpdateEventAdminRequest;

import java.util.List;

@FeignClient(name = "event-service", contextId = "AdminEventFeignClient")
public interface AdminEventFeignClient {
	@GetMapping("/admin/events")
	@ResponseStatus(HttpStatus.OK)
	List<EventFullDto> getEvents(@Valid @ModelAttribute AdminEventParam params);

	@PatchMapping("/admin/events/{eventId}")
	@ResponseStatus(HttpStatus.OK)
	public EventFullDto updateEvents(@PathVariable(name = "eventId") @Positive Long eventId,
									 @RequestBody @Valid UpdateEventAdminRequest dto);
}
