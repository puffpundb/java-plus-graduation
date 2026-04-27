package ru.practicum.commentservice.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.commentservice.service.CommentService;
import ru.practicum.iteractionapi.dto.comment.CommentAdminDto;
import ru.practicum.iteractionapi.feignapi.commentfeignclient.AdminCommentFeignClient;

import java.util.List;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@Validated
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminCommentController implements AdminCommentFeignClient {
	final CommentService commentService;

	@Override
	@GetMapping
	public List<CommentAdminDto> findAll(@RequestParam(required = false) Long eventId,
										 @RequestParam(required = false) Long userId,
										 @RequestParam(required = false) String text,
										 @RequestParam(defaultValue = "0") @Min(0) Integer from,
										 @RequestParam(defaultValue = "10") @Min(1) Integer size
	) {
		log.info("GET /admin/comments with params: eventId={}, userId={} , text={}", eventId, userId, text);
		Pageable pageable = PageRequest.of(from / size, size);
		return commentService.getAllComments(eventId, userId, text, pageable);
	}

	@Override
	@GetMapping("/{commentId}")
	public CommentAdminDto findOne(@PathVariable(name = "commentId") Long commentId) {
		log.info("GET /admin/comments/{}", commentId);
		return commentService.getCommentById(commentId);
	}

	@Override
	@DeleteMapping("/{commentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteComment(@PathVariable(name = "commentId") @Positive Long commentId) {
		log.info("DELETE /admin/comment/{}", commentId);
		commentService.deleteComment(commentId);
	}
}
