package ru.practicum.userservice.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.iteractionapi.dto.request.NewUserRequest;
import ru.practicum.iteractionapi.dto.user.UserDto;
import ru.practicum.iteractionapi.dto.user.UserShortDto;
import ru.practicum.userservice.entity.User;

@UtilityClass
public class UserMapper {
    public static User fromNewUserDtoToUserEntity(NewUserRequest dto) {
        return User.builder()
                .email(dto.getEmail())
                .name(dto.getName())
                .build();
    }

    public static UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    public static UserShortDto toUserShortDto(User user) {
        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}
