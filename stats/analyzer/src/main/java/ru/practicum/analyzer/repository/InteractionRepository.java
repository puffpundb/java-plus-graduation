package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.analyzer.entity.Interaction;

import java.util.Optional;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {
	Optional<Interaction> findByUserIdAndEventId(Long userId, Long eventId);
}
