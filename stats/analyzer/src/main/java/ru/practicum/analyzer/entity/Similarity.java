package ru.practicum.analyzer.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "similarities")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Similarity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;

	@Column(name = "event1", nullable = false)
	Long event1;

	@Column(name = "event2", nullable = false)
	Long event2;

	@Column(name = "similarity", nullable = false)
	Double similarity;

	@Column(name = "ts", nullable = false)
	Instant ts;
}
