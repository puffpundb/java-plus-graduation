package ru.practicum.iteractionapi.feignapi.requestfeignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service", contextId = "InternalRequestFeignClient")
public interface InternalRequestsFeignClient {
	@GetMapping("/internal/events/{eventId}/confirmed-count")
	Long getConfirmedRequestsCount(@PathVariable Long eventId);

	@GetMapping("/internal/events/confirmed-count")
	Map<Long, Long> getConfirmedRequestsCounts(@RequestParam("eventIds") List<Long> eventIds);
}
