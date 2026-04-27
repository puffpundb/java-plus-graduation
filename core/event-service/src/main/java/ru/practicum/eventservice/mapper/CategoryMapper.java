package ru.practicum.eventservice.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.eventservice.entity.Category;
import ru.practicum.iteractionapi.dto.category.CategoryDto;
import ru.practicum.iteractionapi.dto.event.NewCategoryDto;

@UtilityClass
public class CategoryMapper {
    public CategoryDto toCategoryDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    public Category toCategoryEntity(NewCategoryDto dto) {
        return Category.builder()
                .name(dto.getName())
                .build();
    }
}
