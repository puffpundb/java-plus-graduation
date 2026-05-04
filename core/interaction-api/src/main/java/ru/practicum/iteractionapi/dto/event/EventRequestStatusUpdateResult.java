package ru.practicum.iteractionapi.dto.event;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import ru.practicum.iteractionapi.dto.request.ParticipationRequestDto;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class EventRequestStatusUpdateResult {
    // Результат подтверждения/отклонения заявок на участие в событии
    List<ParticipationRequestDto> confirmedRequests;

    List<ParticipationRequestDto> rejectedRequests;
}
