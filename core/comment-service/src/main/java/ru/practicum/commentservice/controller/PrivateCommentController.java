package ru.practicum.commentservice.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.commentservice.service.CommentService;
import ru.practicum.iteractionapi.dto.comment.CommentDto;
import ru.practicum.iteractionapi.dto.comment.CommentRequestDto;
import ru.practicum.iteractionapi.feignapi.commentfeignclient.PrivateCommentFeignClient;


@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(path = "/users/{userId}/events/{eventId}/comments")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrivateCommentController {
	final CommentService commentService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CommentDto createComment(@PathVariable(value = "userId") Long userId,
									@PathVariable(value = "eventId") Long eventId,
									@RequestBody @Valid CommentRequestDto commentRequestDto) {
		return commentService.createComment(userId, eventId, commentRequestDto);
	}

	@PostMapping("/{commentId}/likes")
	public void addAndDeleteLikeComment(@PathVariable(value = "userId") Long userId,
										@PathVariable(value = "eventId") Long eventId,
										@PathVariable(value = "commentId") Long commentId) {
		commentService.addAndDeleteLikeComment(userId, eventId, commentId);
	}

	@PatchMapping("/{commentId}")
	public CommentDto updateComment(@PathVariable(value = "userId") Long userId,
									@PathVariable(value = "eventId") Long eventId,
									@PathVariable(value = "commentId") Long commentId,
									@RequestBody @Valid CommentRequestDto commentRequestDto) {
		return commentService.updateComment(userId, eventId, commentId, commentRequestDto);
	}

	@DeleteMapping("/{commentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteComment(@PathVariable(value = "userId") Long userId,
							  @PathVariable(value = "eventId") Long eventId,
							  @PathVariable(value = "commentId") Long commentId) {
		commentService.deleteComment(userId, eventId, commentId);
	}
}
