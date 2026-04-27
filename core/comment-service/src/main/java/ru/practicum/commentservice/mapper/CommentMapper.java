package ru.practicum.commentservice.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.commentservice.entity.Comment;
import ru.practicum.iteractionapi.dto.comment.CommentAdminDto;
import ru.practicum.iteractionapi.dto.comment.CommentDto;
import ru.practicum.iteractionapi.dto.comment.CommentRequestDto;

@UtilityClass
public class CommentMapper {
    public Comment commentDtoToComment(CommentRequestDto commentRequestDto, Long user, Long event) {
        return Comment.builder()
                .text(commentRequestDto.getText())
                .author(user)
                .event(event)
                .build();
    }

    public CommentDto commentToCommentDto(Comment comment, String userName) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorName(userName)
                .eventId(comment.getEvent())
                .create(comment.getCreatedOn())
                .build();
    }

    public CommentAdminDto commentToCommentAdminDto(Comment comment) {
        return CommentAdminDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorId(comment.getAuthor())
                .eventId(comment.getEvent())
                .createdOn(comment.getCreatedOn())
                .likesCount(comment.getLikesCount())
                .build();
    }

    public CommentDto toCommentDtoWithLikes(Comment comment, Integer likes, String userName) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorName(userName)
                .eventId(comment.getEvent())
                .create(comment.getCreatedOn())
                .like(likes)
                .build();
    }
}
