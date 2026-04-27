package ru.practicum.eventservice.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.eventservice.entity.Category;
import ru.practicum.eventservice.entity.Event;
import ru.practicum.iteractionapi.dto.enums.UserStateAction;
import ru.practicum.iteractionapi.dto.event.*;
import ru.practicum.iteractionapi.dto.user.UserDto;
import ru.practicum.iteractionapi.dto.user.UserShortDto;
import ru.practicum.iteractionapi.model.enums.State;

import java.util.Optional;

@UtilityClass
public class EventMapper {
    private UserShortDto toUserShortDto(UserDto userDto) {
        return UserShortDto.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .build();
    }

    public static EventShortDto toEventShortDto(Event event, UserDto initiator) {
        return EventShortDto.builder()
                .annotation(event.getAnnotation())
                .categoryDto(CategoryMapper.toCategoryDto(event.getCategory()))
                .confirmedRequests(event.getConfirmedRequests())
                .eventDate(event.getEventDate())
                .id(event.getId())
                .initiatorDto(toUserShortDto(initiator))
                .paid(event.getPaid())
                .title(event.getTitle())
                .build();
    }

    public static EventShortDto toEventShortDto(Event event, UserDto initiator, Long views) {
        return EventShortDto.builder()
                .annotation(event.getAnnotation())
                .categoryDto(CategoryMapper.toCategoryDto(event.getCategory()))
                .confirmedRequests(event.getConfirmedRequests())
                .eventDate(event.getEventDate())
                .id(event.getId())
                .initiatorDto(toUserShortDto(initiator))
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(views)
                .build();
    }

    public static Event newEventDtoToEvent(NewEventDto newEventDto, Long userId, Category category) {
        return Event.builder()
                .annotation(newEventDto.getAnnotation())
                .category(category)
                .initiatorId(userId)
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())
                .location(LocationMapper.locationDtoToLocation(newEventDto.getLocationDto()))
                .paid(newEventDto.getPaid())
                .participantLimit(newEventDto.getParticipantLimit())
                .requestModeration(newEventDto.getRequestModeration())
                .title(newEventDto.getTitle())
                .build();
    }

    public static EventFullDto eventToEventFullDto(Event event, UserDto initiator) {
        return EventFullDto.builder()
                .annotation(event.getAnnotation())
                .categoryDto(CategoryMapper.toCategoryDto(event.getCategory()))
                .eventDate(event.getEventDate())
                .confirmedRequests(event.getConfirmedRequests())
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .locationDto(LocationMapper.locationToLocationDto(event.getLocation()))
                .initiatorDto(toUserShortDto(initiator))
                .state(event.getState())
                .id(event.getId())
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .title(event.getTitle())
                .build();
    }

    public static Event updateEventDtoToEvent(Event event, UpdateEventUserRequest update, Optional<Category> category) {
        Optional.ofNullable(update.getAnnotation()).ifPresent(event::setAnnotation);
        Optional.ofNullable(update.getDescription()).ifPresent(event::setDescription);
        Optional.ofNullable(update.getTitle()).ifPresent(event::setTitle);
        Optional.ofNullable(update.getEventDate()).ifPresent(event::setEventDate);
        Optional.ofNullable(update.getPaid()).ifPresent(event::setPaid);
        Optional.ofNullable(update.getParticipantLimit()).ifPresent(event::setParticipantLimit);
        Optional.ofNullable(update.getRequestModeration()).ifPresent(event::setRequestModeration);
        category.ifPresent(event::setCategory);
        LocationDto locationDto = update.getLocationDto();
        if (locationDto != null) {
            event.setLocation(LocationMapper.locationDtoToLocation(locationDto));
        }
        UserStateAction state = update.getStateAction();
        if (UserStateAction.CANCEL_REVIEW.equals(state)) {
            event.setState(State.CANCELED);
        }
        if (UserStateAction.SEND_TO_REVIEW.equals(state)) {
            event.setState(State.PENDING);
        }
        if (state == null) {
            event.setState(State.PENDING);
        }

        return event;
    }

    public static EventFullDto toEventFullDto(Event event, UserDto initiator, Long views) {
        return EventFullDto.builder()
                .annotation(event.getAnnotation())
                .categoryDto(CategoryMapper.toCategoryDto(event.getCategory()))
                .eventDate(event.getEventDate())
                .confirmedRequests(event.getConfirmedRequests())
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .locationDto(LocationMapper.locationToLocationDto(event.getLocation()))
                .initiatorDto(toUserShortDto(initiator))
                .state(event.getState())
                .id(event.getId())
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .title(event.getTitle())
                .views(views)
                .build();
    }

    public static Long extractIdFromUri(String uri) {
        String[] parts = uri.split("/");
        String id = parts[parts.length - 1];

        return Long.parseLong(id);
    }
}
