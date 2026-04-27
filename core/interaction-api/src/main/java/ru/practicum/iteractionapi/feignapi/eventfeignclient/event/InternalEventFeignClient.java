package ru.practicum.iteractionapi.feignapi.eventfeignclient.event;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "event-service")
@RequestMapping("/internal/events")
public interface InternalEventFeignClient {
	@PatchMapping("/{eventId}/confirmed-requests")
	void setConfirmedRequests(@PathVariable Long eventId,
							  @RequestParam Long confirmedRequests);
}
