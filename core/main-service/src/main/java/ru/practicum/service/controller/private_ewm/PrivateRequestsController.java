package ru.practicum.service.controller.private_ewm;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.service.dto.ParticipationRequestDto;
import ru.practicum.service.service.EventService;

import java.util.List;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(path = "/users/{userId}/requests")
public class PrivateRequestsController {
    private final EventService service;

    @GetMapping
    public List<ParticipationRequestDto> getInfoOnParticipation(
            @PathVariable(value = "userId") Long userId) {
        return service.getInfoOnParticipation(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequestForParticipation(
            @PathVariable(value = "userId") Long userId,
            @RequestParam(value = "eventId", required = true) Long eventId,
            HttpServletRequest request) {
        return service.createRequestForParticipation(userId, eventId);
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto canceledRequestForParticipation(
            @PathVariable(value = "userId") Long userId,
            @PathVariable(value = "requestId") Long requestId) {
        return service.canceledRequestForParticipation(userId, requestId);
    }
}
