package ru.practicum.iteractionapi.feignapi.eventfeignclient.event;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;
import ru.practicum.iteractionapi.dto.event.EventFullDto;
import ru.practicum.iteractionapi.dto.event.EventSearchParams;
import ru.practicum.iteractionapi.dto.event.EventShortDto;

import java.util.List;

@FeignClient(name = "event-service", contextId = "InternalEventFeignClient")
public interface InternalEventFeignClient {
	@GetMapping("/events")
	List<EventShortDto> getEvents(@SpringQueryMap @Valid EventSearchParams params);

	@GetMapping("/internal/events/{id}")
	EventFullDto getEventById(@PathVariable(value = "id") Long id);
}
