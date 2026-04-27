package ru.practicum.eventservice.controller.category;


import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.eventservice.service.CategoryService;
import ru.practicum.iteractionapi.dto.category.CategoryDto;
import ru.practicum.iteractionapi.feignapi.categoryfeignclient.PublicCategoryFeignClient;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Validated
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PublicCategoryController {
	final CategoryService categoryService;

	@GetMapping
	public List<CategoryDto> getCategoryByParam(Integer from, Integer size, HttpServletRequest request) {
		log.info("PublicCategoryController: вызов эндпоинта GET /category с параметрами -- from:{}, size:{}", from, size);

		return categoryService.getCategories(from, size, request);
	}

	@GetMapping("/{catId}")
	public CategoryDto getCategory(Long catId, HttpServletRequest request) {
		log.info("PublicCategoryController: вызов эндпоинта GET /category/{}", catId);

		return categoryService.getById(catId, request);
	}
}
