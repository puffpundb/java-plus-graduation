package ru.practicum.service.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.service.dal.CommentLikeRepository;
import ru.practicum.service.dal.CommentRepository;
import ru.practicum.service.dal.EventRepository;
import ru.practicum.service.dal.UserRepository;
import ru.practicum.service.dto.CommentAdminDto;
import ru.practicum.service.dto.CommentDto;
import ru.practicum.service.dto.CommentRequestDto;
import ru.practicum.service.error.ConflictException;
import ru.practicum.service.error.NotFoundException;
import ru.practicum.service.mapper.CommentMapper;
import ru.practicum.service.model.Comment;
import ru.practicum.service.model.CommentLike;
import ru.practicum.service.model.Event;
import ru.practicum.service.model.User;
import ru.practicum.service.model.enums.State;

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
	final EventRepository eventRepository;
	final UserRepository userRepository;

	public List<CommentDto> getCommentByEventId(Long eventId, Integer from, Integer size) {
		log.info("PublicCommentServiceImpl: Поиск комментов с заданными параметрами");
		Pageable pageable = PageRequest.of(from / size, size);
		List<Comment> commentList = commentRepository.findByEvent_IdOrderByCreatedOnDesc(eventId, pageable);
		log.info("PublicCommentServiceImpl: {}", commentList);

		log.info("PublicCommentServiceImpl: Поиск лайков комментов");
		List<Long> commentsIds = commentList.stream().map(Comment::getId).toList();
		List<CommentLike> commentLikeList = commentLikeRepository.findByCommentIdIn(commentsIds);

		Map<Long, Integer> commentLikesMap = commentLikeList.stream()
				.collect(Collectors.groupingBy(CommentLike::getCommentId, Collectors.summingInt(like -> 1)));
		log.info("PublicCommentServiceImpl: {}", commentLikesMap);

		return commentList.stream()
				.map(comment ->
						CommentMapper.toCommentDtoWithLikes(comment, commentLikesMap.getOrDefault(comment.getId(), 0)))
				.toList();
	}

	public CommentDto createComment(Long userId, Long eventId, CommentRequestDto commentRequestDto) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("Такого пользователя не существует."));
		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new NotFoundException("Такого события не найдено."));
		if (event.getState().equals(State.PENDING)) {
			throw new NotFoundException("Такого события не найдено.");
		}
		if (event.getInitiator().getId().equals(userId)) {
			throw new ConflictException("Нельзя комментировать свое событие.");
		}

		return CommentMapper
				.commentToCommentDto(
						commentRepository.save(CommentMapper.commentDtoToComment(commentRequestDto, user, event)));
	}

	public CommentDto updateComment(Long userId, Long eventId, Long commentId, CommentRequestDto commentRequestDto) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("Такого пользователя не существует."));
		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new NotFoundException("Такого события не найдено."));
		if (event.getInitiator().getId().equals(userId)) {
			throw new ConflictException("Нельзя комментировать свое событие.");
		}
		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new NotFoundException("Комментарий не найден."));

		comment.setText(commentRequestDto.getText());

		return CommentMapper.commentToCommentDto(commentRepository.save(comment));
	}

	public void deleteComment(Long userId, Long eventId, Long commentId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("Такого пользователя не существует."));
		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new NotFoundException("Такого события не найдено."));
		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new NotFoundException("Комментарий не найден."));
		if (comment.getAuthor().getId().equals(userId)) {
			commentRepository.delete(comment);
		} else {
			throw new ConflictException("Невозможно удалить чужой комментарий.");
		}
	}

	public void addAndDeleteLikeComment(Long userId, Long eventId, Long commentId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("Такого пользователя не существует."));
		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new NotFoundException("Комментарий не найден."));
		if (userId.equals(comment.getAuthor().getId())) {
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
