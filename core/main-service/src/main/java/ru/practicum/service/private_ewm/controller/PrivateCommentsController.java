package ru.practicum.service.private_ewm.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.service.dto.CommentDto;
import ru.practicum.service.dto.CommentRequestDto;
import ru.practicum.service.private_ewm.service.PrivateServiceComments;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(path = "/users/{userId}/events/{eventId}/comments")
public class PrivateCommentsController {
    private final PrivateServiceComments privateServiceComments;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@PathVariable(value = "userId") Long userId,
                                    @PathVariable(value = "eventId") Long eventId,
                                    @RequestBody @Valid CommentRequestDto commentRequestDto) {
        return privateServiceComments.createComment(userId, eventId, commentRequestDto);
    }

    @PostMapping("/{commentId}/likes")
    public void addAndDeleteLikeComment(@PathVariable(value = "userId") Long userId,
                                        @PathVariable(value = "eventId") Long eventId,
                                        @PathVariable(value = "commentId") Long commentId) {
        privateServiceComments.addAndDeleteLikeComment(userId, eventId, commentId);
    }

    @PatchMapping("/{commentId}")
    public CommentDto updateComment(@PathVariable(value = "userId") Long userId,
                                    @PathVariable(value = "eventId") Long eventId,
                                    @PathVariable(value = "commentId") Long commentId,
                                    @RequestBody @Valid CommentRequestDto commentRequestDto) {
        return privateServiceComments.updateComment(userId, eventId, commentId, commentRequestDto);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable(value = "userId") Long userId,
                              @PathVariable(value = "eventId") Long eventId,
                              @PathVariable(value = "commentId") Long commentId) {
        privateServiceComments.deleteComment(userId, eventId, commentId);
    }
}
