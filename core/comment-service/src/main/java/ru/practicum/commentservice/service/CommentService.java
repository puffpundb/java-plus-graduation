package ru.practicum.commentservice.service;

import io.github.resilience4j.retry.annotation.Retry;
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
import ru.practicum.iteractionapi.feignapi.eventfeignclient.event.InternalEventFeignClient;
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

	final InternalEventFeignClient internalEventFeignClient;
	final UserFeignClient userFeignClient;

	@Retry(name = "commentServiceRetry", fallbackMethod = "fallbackFindUsers")
	protected List<UserDto> findUsers(List<Long> ids, int from, int size) {
		return userFeignClient.findUsers(ids, from, size);
	}

	private List<UserDto> fallbackFindUsers(List<Long> ids, int from, int size, Throwable t) {
		log.warn("User-service unavailable, returning empty user list. Cause: {}", t.getMessage());
		return Collections.emptyList();
	}

	@Retry(name = "commentServiceRetry", fallbackMethod = "fallbackGetEventById")
	protected EventFullDto getEventById(Long eventId) {
		return internalEventFeignClient.getEventById(eventId);
	}

	private EventFullDto fallbackGetEventById(Long eventId, Throwable t) {
		log.error("Event-service unavailable, cannot fetch event {}", eventId, t);
		throw new RuntimeException("Event-service is temporarily unavailable. Please try later.");
	}

	public List<CommentDto> getCommentByEventId(Long eventId, Integer from, Integer size) {
		log.info("PublicCommentServiceImpl: Поиск комментов с заданными параметрами");
		Pageable pageable = PageRequest.of(from / size, size);
		List<Comment> commentList = commentRepository.findByEventIdOrderByCreatedOnDesc(eventId, pageable);
		log.info("PublicCommentServiceImpl: найдено комментариев: {}", commentList.size());

		if (commentList.isEmpty()) return Collections.emptyList();

		List<Long> authorsIds = commentList.stream().map(Comment::getAuthorId).distinct().toList();
		List<UserDto> users = findUsers(authorsIds, 0, authorsIds.size());

		if (users == null || users.isEmpty()) {
			log.warn("No users returned from user-service, returning empty comment list");
			return Collections.emptyList();
		}

		Map<Long, String> authorNames = users.stream()
				.collect(Collectors.toMap(UserDto::getId, UserDto::getName));

		List<Long> commentsIds = commentList.stream().map(Comment::getId).toList();
		List<CommentLike> commentLikeList = commentLikeRepository.findByCommentIdIn(commentsIds);
		Map<Long, Integer> commentLikesMap = commentLikeList.stream()
				.collect(Collectors.groupingBy(CommentLike::getCommentId, Collectors.summingInt(like -> 1)));

		return commentList.stream()
				.map(comment -> {
					String authorName = authorNames.get(comment.getAuthorId());
					Integer likes = commentLikesMap.getOrDefault(comment.getId(), 0);
					return CommentMapper.toCommentDtoWithLikes(comment, likes, authorName);
				})
				.toList();
	}

	private UserDto checkAndGetUser(Long userId) {
		List<UserDto> userDtoList = findUsers(List.of(userId), 0, 1);
		if (userDtoList == null || userDtoList.isEmpty()) {
			throw new NotFoundException("User not found or user-service unavailable: " + userId);
		}
		return userDtoList.getFirst();
	}

	private EventFullDto checkAndGetEvent(Long eventId, Long userId) {
		EventFullDto event = getEventById(eventId);
		if (event == null) {
			throw new RuntimeException("Event-service unavailable");
		}
		if (event.getState().equals(State.PENDING)) {
			throw new NotFoundException("Событие не найдено или ещё не опубликовано");
		}
		if (userId != null && event.getInitiatorDto().getId().equals(userId)) {
			throw new ConflictException("Нельзя комментировать свое событие.");
		}
		return event;
	}

	public CommentDto createComment(Long userId, Long eventId, CommentRequestDto commentRequestDto) {
		UserDto userDto = checkAndGetUser(userId);
		checkAndGetEvent(eventId, userId);
		return CommentMapper.commentToCommentDto(
				commentRepository.save(CommentMapper.commentDtoToComment(commentRequestDto, userId, eventId)),
				userDto.getName());
	}

	public CommentDto updateComment(Long userId, Long eventId, Long commentId, CommentRequestDto commentRequestDto) {
		UserDto userDto = checkAndGetUser(userId);
		checkAndGetEvent(eventId, userId);
		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new NotFoundException("Комментарий не найден."));
		comment.setText(commentRequestDto.getText());
		return CommentMapper.commentToCommentDto(commentRepository.save(comment), userDto.getName());
	}

	public void deleteComment(Long userId, Long eventId, Long commentId) {
		checkAndGetUser(userId);
		checkAndGetEvent(eventId, null);
		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new NotFoundException("Комментарий не найден."));
		if (comment.getAuthorId().equals(userId)) {
			commentRepository.delete(comment);
		} else {
			throw new ConflictException("Невозможно удалить чужой комментарий.");
		}
	}

	public void addAndDeleteLikeComment(Long userId, Long eventId, Long commentId) {
		checkAndGetUser(userId);
		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new NotFoundException("Комментарий не найден."));
		if (userId.equals(comment.getAuthorId())) {
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
			comments = commentRepository.findByEventIdOrderByCreatedOnDesc(eventId, pageable);
		} else if (userId != null) {
			comments = commentRepository.findAllByAuthorIdOrderByCreatedOnDesc(userId, pageable);
		} else if (text != null && !text.isEmpty()) {
			comments = commentRepository.findByTextContainingIgnoreCaseOrderByCreatedOnDesc(text, pageable);
		} else {
			comments = commentRepository.findAll(pageable).getContent();
		}
		return comments.stream().map(CommentMapper::commentToCommentAdminDto).toList();
	}

	public CommentAdminDto getCommentById(Long commentId) {
		Comment comment = findCommentOrThrow(commentId);
		return CommentMapper.commentToCommentAdminDto(comment);
	}

	@Transactional
	public void deleteComment(Long commentId) {
		Comment comment = findCommentOrThrow(commentId);
		commentRepository.delete(comment);
	}

	private Comment findCommentOrThrow(Long commentId) {
		return commentRepository.findById(commentId)
				.orElseThrow(() -> new NotFoundException("Комментарий с id: " + commentId + " не найден"));
	}
}
