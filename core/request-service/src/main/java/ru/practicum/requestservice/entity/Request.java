package ru.practicum.requestservice.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.iteractionapi.model.enums.Status;

import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    LocalDateTime created = LocalDateTime.now();

    @Column(name = "event_id")
    @ToString.Exclude
    private Long event;

    @Column(name = "requester_id")
    @ToString.Exclude
    private Long requester;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Request)) return false;
        return id != null && id.equals(((Request) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
