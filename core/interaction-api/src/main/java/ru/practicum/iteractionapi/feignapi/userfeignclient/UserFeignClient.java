package ru.practicum.iteractionapi.feignapi.userfeignclient;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.iteractionapi.dto.request.NewUserRequest;
import ru.practicum.iteractionapi.dto.user.UserDto;

import java.util.List;

@FeignClient(name = "user-service")
@RequestMapping("/admin/users")
public interface UserFeignClient {
	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public List<UserDto> findUsers(@RequestParam(required = false) List<Long> ids,
								   @RequestParam(name = "from", defaultValue = "0") @PositiveOrZero Integer from,
								   @RequestParam(name = "size", defaultValue = "10") @Positive Integer size);

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UserDto newUser(@RequestBody @Valid NewUserRequest dto);

	@DeleteMapping("/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteUser(@PathVariable(name = "userId") @Positive Long userId);
}
