package ru.practicum.eventservice.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.eventservice.entity.Compilation;
import ru.practicum.iteractionapi.dto.event.CompilationDto;
import ru.practicum.iteractionapi.dto.event.EventShortDto;
import ru.practicum.iteractionapi.dto.event.NewCompilationDto;

import java.util.HashSet;
import java.util.Set;

@UtilityClass
public class CompilationMapper {
    public Compilation newCompilationToEntity(NewCompilationDto dto) {
        return Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.getPinned())
                .events(new HashSet<>())
                .build();
    }

    public CompilationDto toCompilationDto(Compilation compilation, Set<EventShortDto> eventsDto) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .events(eventsDto)
                .build();
    }
}
