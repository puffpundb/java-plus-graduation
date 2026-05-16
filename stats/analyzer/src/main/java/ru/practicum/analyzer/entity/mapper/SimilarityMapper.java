package ru.practicum.analyzer.entity.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.analyzer.entity.Similarity;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Component
public class SimilarityMapper {
	public Similarity toSimilarity(EventSimilarityAvro avro) {
		return Similarity.builder()
				.event1(avro.getEventA())
				.event2(avro.getEventB())
				.similarity(avro.getScore())
				.ts(avro.getTimestamp())
				.build();
	}
}
