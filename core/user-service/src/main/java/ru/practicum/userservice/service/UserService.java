package ru.practicum.userservice.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.iteractionapi.dto.request.NewUserRequest;
import ru.practicum.iteractionapi.dto.user.UserDto;
import ru.practicum.iteractionapi.error.ConflictException;
import ru.practicum.iteractionapi.error.NotFoundException;
import ru.practicum.userservice.entity.User;
import ru.practicum.userservice.mapper.UserMapper;
import ru.practicum.userservice.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserService {
	final UserRepository repository;

	@Transactional(readOnly = true)
	public List<UserDto> getUsers(List<Long> ids, Integer from, Integer size) {
		int safeFrom = (from != null && from >= 0) ? from : 0;
		int safeSize = (size != null && size > 0) ? size : 10;

		int pageNumber = safeFrom / safeSize;
		Pageable pageable = PageRequest.of(pageNumber, safeSize);

		Page<User> users;
		if (ids == null || ids.isEmpty()) {
			users = repository.findAll(pageable);
		} else {
			users = repository.findByIdIn(ids, pageable);
		}

		return users.getContent().stream()
				.map(UserMapper::toUserDto)
				.toList();
	}

	@Transactional
	public UserDto createUser(NewUserRequest dto) {
		if (repository.existsByEmail(dto.getEmail())) {
			throw new ConflictException("Адрес электронной почты уже существует.");
		}

		User user = UserMapper.fromNewUserDtoToUserEntity(dto);
		User savedUser = repository.save(user);

		return UserMapper.toUserDto(savedUser);
	}

	@Transactional
	public void deleteUser(Long id) {
		if (!repository.existsById(id)) {
			throw new NotFoundException("Пользователь с id=" + id + " не найден.");
		}
		repository.deleteById(id);
	}
}
