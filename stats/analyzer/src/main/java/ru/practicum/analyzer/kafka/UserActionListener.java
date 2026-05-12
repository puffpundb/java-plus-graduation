package ru.practicum.analyzer.kafka;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.service.AnalyzerDataBaseService;
import ru.practicum.analyzer.service.AnalyzerService;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserActionListener {
	final AnalyzerDataBaseService analyzerDataBaseService;

	@KafkaListener(
			topics = "${spring.kafka.topics.user-actions}",
			groupId = "${analyzer.kafka.user-group}",
			containerFactory = "userActionContainerFactory"
	)
	public void listen(UserActionAvro a) {
		analyzerDataBaseService.saveInteraction(a);
	}
}
