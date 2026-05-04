package ru.practicum.requestservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.requestservice.entity.EventConfirmedCount;
import ru.practicum.iteractionapi.model.enums.Status;
import ru.practicum.requestservice.entity.Request;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findAllByEventId(Long eventId);

    List<Request> findAllByRequesterId(Long userId);

    Optional<Request> findByEventIdAndRequesterId(Long eventId, Long userId);

    long countByEventIdAndStatus(Long eventId, Status status);

    @Query("SELECT new ru.practicum.requestservice.entity.EventConfirmedCount(r.eventId, COUNT(r)) " +
            "FROM Request r WHERE r.eventId IN :eventIds AND r.status = :status GROUP BY r.eventId")
    List<EventConfirmedCount> countByEventIdInAndStatus(@Param("eventIds") List<Long> eventIds,
                                                        @Param("status") Status status);
}
