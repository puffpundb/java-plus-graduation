package ru.practicum.iteractionapi.feignapi.commentfeignclient;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.iteractionapi.dto.comment.CommentDto;
import ru.practicum.iteractionapi.dto.comment.CommentRequestDto;

@FeignClient(name = "comment-service", contextId = "PrivateCommentFeignClient")
public interface PrivateCommentFeignClient {
	@PostMapping("/users/{userId}/events/{eventId}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	CommentDto createComment(@PathVariable(value = "userId") Long userId,
							 @PathVariable(value = "eventId") Long eventId,
							 @RequestBody @Valid CommentRequestDto commentRequestDto);

	@PostMapping("/users/{userId}/events/{eventId}/comments/{commentId}/likes")
	void addAndDeleteLikeComment(@PathVariable(value = "userId") Long userId,
								 @PathVariable(value = "eventId") Long eventId,
								 @PathVariable(value = "commentId") Long commentId);

	@PatchMapping("/users/{userId}/events/{eventId}/comments/{commentId}")
	CommentDto updateComment(@PathVariable(value = "userId") Long userId,
							 @PathVariable(value = "eventId") Long eventId,
							 @PathVariable(value = "commentId") Long commentId,
							 @RequestBody @Valid CommentRequestDto commentRequestDto);

	@DeleteMapping("/users/{userId}/events/{eventId}/comments/{commentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteComment(@PathVariable(value = "userId") Long userId,
					   @PathVariable(value = "eventId") Long eventId,
					   @PathVariable(value = "commentId") Long commentId);
}
