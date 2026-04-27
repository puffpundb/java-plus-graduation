package ru.practicum.commentservice.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.commentservice.entity.Comment;
import ru.practicum.commentservice.entity.CommentLike;
import ru.practicum.commentservice.mapper.CommentMapper;
import ru.practicum.commentservice.repository.CommentLikeRepository;
import ru.practicum.commentservice.repository.CommentRepository;
import ru.practicum.iteractionapi.dto.comment.CommentAdminDto;
import ru.practicum.iteractionapi.dto.comment.CommentDto;
import ru.practicum.iteractionapi.dto.comment.CommentRequestDto;
import ru.practicum.iteractionapi.dto.event.EventFullDto;
import ru.practicum.iteractionapi.dto.user.UserDto;
import ru.practicum.iteractionapi.error.ConflictException;
import ru.practicum.iteractionapi.error.NotFoundException;
import ru.practicum.iteractionapi.feignapi.eventfeignclient.event.PublicEventFeignClient;
import ru.practicum.iteractionapi.feignapi.userfeignclient.UserFeignClient;
import ru.practicum.iteractionapi.model.enums.State;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class CommentService {
	final CommentRepository commentRepository;
	final CommentLikeRepository commentLikeRepository;

	final PublicEventFeignClient publicEventFeignClient;
	final UserFeignClient userFeignClient;

	public List<CommentDto> getCommentByEventId(Long eventId, Integer from, Integer size) {
		log.info("PublicCommentServiceImpl: Поиск комментов с заданными параметрами");
		Pageable pageable = PageRequest.of(from / size, size);
		List<Comment> commentList = commentRepository.findByEvent_IdOrderByCreatedOnDesc(eventId, pageable);
		log.info("PublicCommentServiceImpl: {}", commentList);

		if (commentList.isEmpty()) return Collections.emptyList();

		List<Long> authorsIds = commentList.stream().map(Comment::getAuthor).distinct().toList();
		List<UserDto> users = userFeignClient.findUsers(authorsIds, 0, authorsIds.size());

		Map<Long, String> authorNames = users.stream().collect(Collectors.toMap(UserDto::getId, UserDto::getName));

		log.info("PublicCommentServiceImpl: Поиск лайков комментов");
		List<Long> commentsIds = commentList.stream().map(Comment::getId).toList();
		List<CommentLike> commentLikeList = commentLikeRepository.findByCommentIdIn(commentsIds);

		Map<Long, Integer> commentLikesMap = commentLikeList.stream()
				.collect(Collectors.groupingBy(CommentLike::getCommentId, Collectors.summingInt(like -> 1)));
		log.info("PublicCommentServiceImpl: {}", commentLikesMap);

		return commentList.stream()
				.map(comment -> {
					String authorName = authorNames.getOrDefault(comment.getAuthor(), "Unknown");
					Integer likes = commentLikesMap.getOrDefault(comment.getId(), 0);
					return CommentMapper.toCommentDtoWithLikes(comment, likes, authorName);
				})
				.toList();
	}

	private UserDto checkAndGetUser(Long userId) {
		List<UserDto> userDtoList = userFeignClient.findUsers(List.of(userId), 0, 1);
		if (userDtoList.isEmpty()) throw new NotFoundException("Такого пользователя не существует.");

		return userDtoList.getFirst();
	}

	private void checkEvent(Long eventId, Long userId) {
		EventFullDto event = publicEventFeignClient.getEventById(eventId, null);

		if (event.getState().equals(State.PENDING)) {
			throw new NotFoundException("Такого события не найдено.");
		}
		if (event.getInitiatorDto().getId().equals(userId)) {
			throw new ConflictException("Нельзя комментировать свое событие.");
		}
	}

	public CommentDto createComment(Long userId, Long eventId, CommentRequestDto commentRequestDto) {
		UserDto userDto = checkAndGetUser(userId);
		checkEvent(eventId, userId);


		return CommentMapper
				.commentToCommentDto(commentRepository.save(CommentMapper.commentDtoToComment(commentRequestDto, userId, eventId)),
						userDto.getName());
	}

	public CommentDto updateComment(Long userId, Long eventId, Long commentId, CommentRequestDto commentRequestDto) {
		UserDto userDto = checkAndGetUser(userId);
		checkEvent(eventId, userId);
		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new NotFoundException("Комментарий не найден."));

		comment.setText(commentRequestDto.getText());

		return CommentMapper.commentToCommentDto(commentRepository.save(comment), userDto.getName());
	}

	public void deleteComment(Long userId, Long eventId, Long commentId) {
		checkAndGetUser(userId);
		checkEvent(eventId, null);
		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new NotFoundException("Комментарий не найден."));

		if (comment.getAuthor().equals(userId)) {
			commentRepository.delete(comment);
		} else {
			throw new ConflictException("Невозможно удалить чужой комментарий.");
		}
	}

	public void addAndDeleteLikeComment(Long userId, Long eventId, Long commentId) {
		checkAndGetUser(userId);
		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new NotFoundException("Комментарий не найден."));
		if (userId.equals(comment.getAuthor())) {
			throw new ConflictException("Невозможно поставить лайк на свой комментарий.");
		}

		CommentLike like = CommentLike.builder()
				.userId(userId)
				.commentId(commentId)
				.build();
		boolean exists = commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
		if (!exists) {
			commentLikeRepository.save(like);
		} else {
			commentLikeRepository.delete(like);
		}
	}

	public List<CommentAdminDto> getAllComments(Long eventId, Long userId, String text, Pageable pageable) {

		List<Comment> comments;

		if (eventId != null && userId != null) {
			comments = commentRepository.findAllByEventIdAndAuthorIdOrderByCreatedOnDesc(eventId, userId, pageable);
		} else if (eventId != null) {
			comments = commentRepository.findByEvent_IdOrderByCreatedOnDesc(eventId, pageable);
		} else if (userId != null) {
			comments = commentRepository.findAllByAuthorIdOrderByCreatedOnDesc(userId, pageable);
		} else if (text != null && !text.isEmpty()) {
			comments = commentRepository.findByTextContainingIgnoreCaseOrderByCreatedOnDesc(text, pageable);
		} else {
			comments = commentRepository.findAll(pageable).getContent();
		}

		log.info("Найден список комментариев. Всего: {}", comments.size());
		return comments.stream()
				.map(CommentMapper::commentToCommentAdminDto)
				.toList();
	}

	public CommentAdminDto getCommentById(Long commentId) {
		Comment comment =  findCommentOrThrow(commentId);
		log.info("Найден комментарий: {}",  comment);
		return CommentMapper.commentToCommentAdminDto(comment);
	}

	@Transactional
	public void deleteComment(Long commentId) {
		Comment comment = findCommentOrThrow(commentId);
		commentRepository.delete(comment);
		log.info("Комментарий с id={} успешно удален",  commentId);
	}

	private Comment findCommentOrThrow(Long commentId) {
		return commentRepository.findById(commentId)
				.orElseThrow(() -> {
					log.error("Комментарий с id: {} не найден", commentId);
					return new NotFoundException("Комментарий с id: " + commentId + " не найден");
				});
	}
}
