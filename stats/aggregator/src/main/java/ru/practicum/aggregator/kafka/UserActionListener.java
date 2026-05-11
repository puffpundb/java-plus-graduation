package ru.practicum.aggregator.kafka;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.aggregator.service.AggregatorService;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserActionListener {
	final AggregatorService aggregatorService;

	@KafkaListener(
			topics = "${spring.kafka.topics.user-actions}",
			groupId = "${spring.kafka.consumer.group-id}"
	)
	public void listen(UserActionAvro a) {
		aggregatorService.processUserAction(a);
	}
}
