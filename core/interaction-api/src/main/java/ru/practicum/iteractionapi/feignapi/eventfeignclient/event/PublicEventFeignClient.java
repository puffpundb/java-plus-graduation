package ru.practicum.iteractionapi.feignapi.eventfeignclient.event;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.practicum.iteractionapi.dto.event.EventFullDto;
import ru.practicum.iteractionapi.dto.event.EventSearchParams;
import ru.practicum.iteractionapi.dto.event.EventShortDto;

import java.util.List;

@FeignClient(name = "event-service")
@RequestMapping("/events")
public interface PublicEventFeignClient {
	@GetMapping
	public List<EventShortDto> getEvents(@Valid EventSearchParams params, HttpServletRequest request);

	@GetMapping("/{id}")
	public EventFullDto getEventById(@PathVariable(value = "id") Long id, HttpServletRequest request);
}
