package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.entity.Interaction;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {
	Optional<Interaction> findByUserIdAndEventId(Long userId, Long eventId);
	List<Interaction> findByUserId(Long userId);
	List<Interaction> findByUserIdOrderByTsDesc(Long userId);

	@Query("SELECT SUM(i.rating) FROM Interaction i WHERE i.eventId = :eventId")
	Double sumRatingByEventId(@Param("eventId") Long eventId);

	List<Interaction> findAllByEventIdIn(Collection<Long> eventIds);

	@Query("SELECT i FROM Interaction i WHERE i.userId = :userId AND i.eventId IN :eventIds")
	List<Interaction> findAllByUserIdAndEventIdIn(@Param("userId") long userId, @Param("eventIds") Collection<Long> eventIds);
}
