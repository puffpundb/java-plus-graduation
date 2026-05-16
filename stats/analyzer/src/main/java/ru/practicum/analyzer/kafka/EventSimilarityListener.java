package ru.practicum.analyzer.kafka;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.service.AnalyzerDataBaseService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventSimilarityListener {
	final AnalyzerDataBaseService analyzerDataBaseService;

	@KafkaListener(
			topics = "${spring.kafka.topics.event-similarity}",
			groupId = "${analyzer.kafka.similarity-group}",
			containerFactory = "eventSimilarityContainerFactory"
	)
	public void listen(EventSimilarityAvro a) {
		analyzerDataBaseService.saveSimilarity(a);
	}
}
