package ru.practicum.analyzer.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@Table(name = "interactions")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Interaction {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;

	@Column(name = "user_id", nullable = false)
	Long userId;

	@Column(name = "event_id", nullable = false)
	Long eventId;

	@Column(name = "rating", nullable = false)
	Double rating;

	@Column(name = "ts", nullable = false)
	Instant ts;
}
