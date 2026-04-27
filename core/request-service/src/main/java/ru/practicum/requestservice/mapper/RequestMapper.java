package ru.practicum.requestservice.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.iteractionapi.dto.request.ParticipationRequestDto;
import ru.practicum.requestservice.entity.Request;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@UtilityClass
public class RequestMapper {
    public static ParticipationRequestDto toRequestDto(Request request) {
        LocalDateTime created = request.getCreated()
                .truncatedTo(ChronoUnit.MILLIS);

        return ParticipationRequestDto.builder()
                .requesterId(request.getRequesterId())
                .eventId(request.getEventId())
                .created(created)
                .status(request.getStatus())
                .id(request.getId())
                .build();
    }
}
