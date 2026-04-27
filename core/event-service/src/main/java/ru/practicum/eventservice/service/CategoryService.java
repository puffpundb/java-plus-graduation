package ru.practicum.eventservice.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.client.StatClient;
import ru.practicum.dto.request.StatHitRequestDto;
import ru.practicum.eventservice.entity.Category;
import ru.practicum.eventservice.mapper.CategoryMapper;
import ru.practicum.eventservice.repository.CategoryRepository;
import ru.practicum.iteractionapi.dto.Constant;
import ru.practicum.iteractionapi.dto.category.CategoryDto;
import ru.practicum.iteractionapi.dto.event.NewCategoryDto;
import ru.practicum.iteractionapi.error.ConflictException;
import ru.practicum.iteractionapi.error.NotFoundException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class CategoryService {
	final CategoryRepository categoryRepository;
	final StatClient statClient;

	public List<CategoryDto> getCategories(Integer from, Integer size, HttpServletRequest request) {
		log.info("PublicCategoryService: выгрузка категорий по заданным параметрам:");
		Pageable pageable = PageRequest.of(from / size, size);
		List<Category> categoryList = categoryRepository.findAll(pageable).getContent();
		log.info("{}", categoryList);

		statClient.hit(new StatHitRequestDto(Constant.SERVICE_POSTFIX,
				request.getRequestURI(),
				request.getRemoteAddr(),
				LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constant.DATE_TIME_FORMAT)))
		);

		return categoryList.stream().map(CategoryMapper::toCategoryDto).toList();
	}

	public CategoryDto getById(Long catId, HttpServletRequest request) {
		log.info("PublicCategoryService: поиск категории с переданным id:");
		Category category = categoryRepository.findById(catId)
				.orElseThrow(() -> new NotFoundException(String.format("Категория с id: %d не найдена", catId)));
		log.info("{}", category);

		statClient.hit(new StatHitRequestDto(Constant.SERVICE_POSTFIX,
				request.getRequestURI(),
				request.getRemoteAddr(),
				LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constant.DATE_TIME_FORMAT)))
		);

		return CategoryMapper.toCategoryDto(category);
	}

	public CategoryDto createCategory(NewCategoryDto dto) {
		log.info("Попытка создания новой категории с названием: {}", dto.getName());

		if (categoryRepository.existsByName(dto.getName())) {
			log.warn("Ошибка при создании категории: категория с названием '{}' уже существует", dto.getName());
			throw new ConflictException("Категория с названием " + dto.getName() + " уже существует");
		}

		log.debug("Создание сущности Category из DTO: {}", dto);
		Category category = CategoryMapper.toCategoryEntity(dto);

		Category saved = categoryRepository.save(category);

		log.info("Категория успешно создана: id={}, name={}", saved.getId(), saved.getName());
		return CategoryMapper.toCategoryDto(saved);
	}

	public void deleteCategory(Long catId) {
		log.info("Попытка удаления категории с id={}", catId);

		Category category = categoryRepository.findById(catId)
				.orElseThrow(() -> {
					log.error("Категория с id={} не найдена", catId);
					return new NotFoundException("Категория с id=" + catId + " не найдена");
				});

		log.debug("Проверка связанных событий для категории id={}", catId);
		long eventsCount = categoryRepository.countEventsByCategoryId(catId);
		if (eventsCount > 0) {
			log.warn("Нельзя удалить категорию id={}: связано {} событий", catId, eventsCount);
			throw new ConflictException("Нельзя удалить категорию с id=" + catId +
					" так как с ней связаны события. Количество: " + eventsCount);
		}

		log.debug("Удаление категории id={} из базы данных", catId);
		categoryRepository.delete(category);

		log.info("Категория с id={} успешно удалена", catId);
	}

	public CategoryDto updateCategory(Long catId, CategoryDto dto) {
		log.info("Попытка обновления категории с id={}. Новое название: {}", catId, dto.getName());

		Category category = categoryRepository.findById(catId)
				.orElseThrow(() -> {
					log.error("Категория с id={} не найдена для обновления", catId);
					return new NotFoundException("Категория с id=" + catId + " не найдена для обновления");
				});

		if (!category.getName().equals(dto.getName()) &&
				categoryRepository.existsByNameAndIdNot(dto.getName(), catId)) {
			log.warn("Ошибка при обновлении категории id={}: название '{}' уже используется",
					catId, dto.getName());
			throw new ConflictException("Категория с названием " + dto.getName() + " уже используется");
		}

		log.info("Изменение названия категории id={} с '{}' на '{}'", catId, category.getName(), dto.getName());
		category.setName(dto.getName());

		log.debug("Сохранение обновленной категории в базу данных");
		Category updated = categoryRepository.save(category);

		log.info("Категория с id={} успешно обновлена", catId);
		return CategoryMapper.toCategoryDto(updated);
	}
}
