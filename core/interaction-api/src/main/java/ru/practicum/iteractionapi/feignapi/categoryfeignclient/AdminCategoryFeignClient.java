package ru.practicum.iteractionapi.feignapi.categoryfeignclient;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.iteractionapi.dto.category.CategoryDto;
import ru.practicum.iteractionapi.dto.event.NewCategoryDto;

@FeignClient(name = "event-service", contextId = "AdminCategoryFeignClient")
public interface AdminCategoryFeignClient {
	@PostMapping("/admin/categories")
	@ResponseStatus(HttpStatus.CREATED)
	CategoryDto newCategory(@RequestBody @Valid NewCategoryDto dto);

	@DeleteMapping("/admin/categories/{catId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteCategory(@PathVariable(name = "catId") @Positive Long catId);

	@PatchMapping("/admin/categories/{catId}")
	CategoryDto updateCategory(@PathVariable(name = "catId") @Positive Long catId,
							   @RequestBody @Valid CategoryDto dto);
}
