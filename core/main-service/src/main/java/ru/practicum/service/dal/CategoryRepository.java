package ru.practicum.service.dal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.service.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByName(String name);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.category.id = :categoryId")
    long countEventsByCategoryId(@Param("categoryId") Long categoryId);
}
