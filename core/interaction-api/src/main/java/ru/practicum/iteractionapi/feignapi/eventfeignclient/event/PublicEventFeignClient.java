package ru.practicum.iteractionapi.feignapi.eventfeignclient.event;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.practicum.iteractionapi.dto.event.EventFullDto;
import ru.practicum.iteractionapi.dto.event.EventSearchParams;
import ru.practicum.iteractionapi.dto.event.EventShortDto;

import java.util.List;

@FeignClient(name = "event-service", contextId = "PublicEventFeignClient")
public interface PublicEventFeignClient {
//	@GetMapping("/events")
//	public List<EventShortDto> getEvents(@SpringQueryMap @Valid EventSearchParams params, HttpServletRequest request);
//
//	@GetMapping("/events/{id}")
//	public EventFullDto getEventById(@PathVariable(value = "id") Long id, HttpServletRequest request);
}
