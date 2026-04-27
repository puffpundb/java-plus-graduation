package ru.practicum.commentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String text;

    @Column(name = "event_id")
    @ToString.Exclude
    private Long event;

    @Column(name = "author_id")
    @ToString.Exclude
    private Long author;

    @Column(name = "created_on")
    @Builder.Default
    LocalDateTime createdOn  = LocalDateTime.now();

    @Column(name = "likes_count")
    @Builder.Default
    private Integer likesCount = 0;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Comment)) return false;
        return id != null && id.equals(((Comment) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
