package ru.practicum.iteractionapi.feignapi.commentfeignclient;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.iteractionapi.dto.comment.CommentAdminDto;

import java.util.List;

@FeignClient(name = "comment-service")
@RequestMapping("/admin/comments")
public interface AdminCommentFeignClient {
	@GetMapping
	List<CommentAdminDto> findAll(@RequestParam(required = false) Long eventId,
								  @RequestParam(required = false) Long userId,
								  @RequestParam(required = false) String text,
								  @RequestParam(defaultValue = "0") @Min(0) Integer from,
								  @RequestParam(defaultValue = "10") @Min(1) Integer size);

	@GetMapping("/{commentId}")
	CommentAdminDto findOne(@PathVariable(name = "commentId") Long commentId);

	@DeleteMapping("/{commentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteComment(@PathVariable(name = "commentId") @Positive Long commentId);
}
