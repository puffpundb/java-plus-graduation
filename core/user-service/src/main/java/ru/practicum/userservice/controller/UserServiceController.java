package ru.practicum.userservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.iteractionapi.dto.request.NewUserRequest;
import ru.practicum.iteractionapi.dto.user.UserDto;
import ru.practicum.iteractionapi.feignapi.userfeignclient.UserFeignClient;
import ru.practicum.userservice.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Validated
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserServiceController {
	final UserService service;

	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public List<UserDto> findUsers(@RequestParam(required = false) List<Long> ids,
								   @RequestParam(name = "from", defaultValue = "0") Integer from,
								   @RequestParam(name = "size", defaultValue = "10") Integer size) {
		log.info("Admin: Получение пользователей: ids={}, from={},size={}", ids, from, size);
		return service.getUsers(ids, from, size);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UserDto newUser(@RequestBody @Valid NewUserRequest dto) {
		log.info("Admin: Добавление нового пользователя: {}", dto);
		return service.createUser(dto);
	}

	@DeleteMapping("/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteUser(@PathVariable(name = "userId") @Positive Long userId) {
		log.info("Admin: Удаление пользователя с id={}", userId);
		service.deleteUser(userId);
	}
}
