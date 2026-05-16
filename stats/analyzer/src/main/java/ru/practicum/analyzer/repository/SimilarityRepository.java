package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.entity.Similarity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SimilarityRepository extends JpaRepository<Similarity, Long> {
	Optional<Similarity> findByEvent1AndEvent2(Long e1, Long e2);
	List<Similarity> findByEvent1OrEvent2(Long event1, Long event2);

	@Query("SELECT s FROM Similarity s WHERE s.event1 IN :eventIdsA OR s.event2 IN :eventIdsB")
	List<Similarity> findAllByEventAInOrEventBIn(@Param("eventIdsA") Collection<Long> eventIdsA,
												 @Param("eventIdsB") Collection<Long> eventIdsB);
}
