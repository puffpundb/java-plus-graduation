package ru.practicum.requestservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.requestservice.entity.Request;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findAllByEventId(Long eventId);

    List<Request> findAllByRequesterId(Long userId);

    Optional<Request> findByEventIdAndRequesterId(Long eventId, Long userId);

}
