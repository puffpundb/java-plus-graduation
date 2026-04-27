package ru.practicum.eventservice.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.eventservice.entity.Location;
import ru.practicum.iteractionapi.dto.event.LocationDto;

@UtilityClass
public class LocationMapper {
    public static Location locationDtoToLocation(LocationDto locationDto) {
        return Location.builder()
                .lat(locationDto.getLat())
                .lon(locationDto.getLon())
                .build();
    }

    public static LocationDto locationToLocationDto(Location location) {
        return LocationDto.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }
}
