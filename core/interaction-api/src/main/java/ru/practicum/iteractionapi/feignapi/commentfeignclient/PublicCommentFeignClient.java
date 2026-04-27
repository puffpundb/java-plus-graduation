package ru.practicum.iteractionapi.feignapi.commentfeignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.iteractionapi.dto.comment.CommentDto;

import java.util.List;

@FeignClient(name = "comment-service")
@RequestMapping("/events/{eventId}/comments")
public interface PublicCommentFeignClient {
	@GetMapping
	List<CommentDto> getCommentForEvent(@PathVariable(value = "eventId") Long eventId,
											   @RequestParam(value = "from", defaultValue = "0") Integer from,
											   @RequestParam(value = "size", defaultValue = "10") Integer size);
}
