package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.analyzer.entity.Similarity;

import java.util.List;
import java.util.Optional;

public interface SimilarityRepository extends JpaRepository<Similarity, Long> {
	Optional<Similarity> findByEvent1AndEvent2(Long e1, Long e2);
	List<Similarity> findByEvent1OrEvent2(Long event1, Long event2);
}
