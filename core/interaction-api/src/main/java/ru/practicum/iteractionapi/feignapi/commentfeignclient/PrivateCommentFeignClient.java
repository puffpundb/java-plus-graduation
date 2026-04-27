package ru.practicum.iteractionapi.feignapi.commentfeignclient;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.iteractionapi.dto.comment.CommentDto;
import ru.practicum.iteractionapi.dto.comment.CommentRequestDto;

@FeignClient(name = "comment-service")
@RequestMapping(path = "/users/{userId}/events/{eventId}/comments")
public interface PrivateCommentFeignClient {
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	CommentDto createComment(@PathVariable(value = "userId") Long userId,
							 @PathVariable(value = "eventId") Long eventId,
							 @RequestBody @Valid CommentRequestDto commentRequestDto);

	@PostMapping("/{commentId}/likes")
	void addAndDeleteLikeComment(@PathVariable(value = "userId") Long userId,
								 @PathVariable(value = "eventId") Long eventId,
								 @PathVariable(value = "commentId") Long commentId);

	@PatchMapping("/{commentId}")
	CommentDto updateComment(@PathVariable(value = "userId") Long userId,
							 @PathVariable(value = "eventId") Long eventId,
							 @PathVariable(value = "commentId") Long commentId,
							 @RequestBody @Valid CommentRequestDto commentRequestDto);

	@DeleteMapping("/{commentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteComment(@PathVariable(value = "userId") Long userId,
					   @PathVariable(value = "eventId") Long eventId,
					   @PathVariable(value = "commentId") Long commentId);
}
