package ru.practicum.aggregator.kafka;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SimilarityProducer {
	final KafkaTemplate<Long, EventSimilarityAvro> kafkaTemplate;

	@Value("${spring.kafka.topics.events-similarity}")
	String topic;

	public void send(EventSimilarityAvro similarityAvro) {
		kafkaTemplate.send(topic, similarityAvro.getEventA(), similarityAvro);
	}
}
