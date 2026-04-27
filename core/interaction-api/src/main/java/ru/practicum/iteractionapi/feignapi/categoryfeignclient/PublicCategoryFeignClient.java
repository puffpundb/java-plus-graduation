package ru.practicum.iteractionapi.feignapi.categoryfeignclient;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.iteractionapi.dto.category.CategoryDto;

import java.util.List;

@FeignClient(name = "event-service", contextId = "PublicCategoryFeignClient")
public interface PublicCategoryFeignClient {
	@GetMapping("/categories")
	List<CategoryDto> getCategoryByParam(@RequestParam(value = "from", defaultValue = "0") @PositiveOrZero Integer from,
										 @RequestParam(value = "size", defaultValue = "10") @Positive Integer size,
										 HttpServletRequest request);

	@GetMapping("/categories/{catId}")
	CategoryDto getCategory(@PathVariable(value = "catId") Long catId, HttpServletRequest request);
}
