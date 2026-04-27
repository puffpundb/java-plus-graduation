package ru.practicum.eventservice.controller.category;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.eventservice.service.CategoryService;
import ru.practicum.iteractionapi.dto.category.CategoryDto;
import ru.practicum.iteractionapi.dto.event.NewCategoryDto;
import ru.practicum.iteractionapi.feignapi.categoryfeignclient.AdminCategoryFeignClient;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Validated
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminCategoryController {
	final CategoryService categoryService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CategoryDto newCategory(@RequestBody @Valid NewCategoryDto dto) {
		log.info("POST admin/categories: {}", dto);
		return categoryService.createCategory(dto);
	}

	@DeleteMapping("/{catId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteCategory(@PathVariable(name = "catId") @Positive Long catId) {
		log.info("DELETE admin/categories/{}", catId);
		categoryService.deleteCategory(catId);
	}

	@PatchMapping("/{catId}")
	public CategoryDto updateCategory(@PathVariable(name = "catId") @Positive Long catId,
									  @RequestBody @Valid CategoryDto dto) {
		log.info("PATCH admin/categories/{}: {}", catId, dto);
		return categoryService.updateCategory(catId, dto);
	}
}
