package ru.practicum.analyzer.kafka;

import deserializer.EventSimilarityAvroDeserializer;
import deserializer.UserActionAvroDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Value("${spring.kafka.consumer.key-deserializer}")
	private String keyDeserializer;

	@Value("${analyzer.kafka.user-group}")
	private String userGroupId;

	@Value("${analyzer.kafka.similarity-group}")
	private String similarityGroupId;

	@Bean
	public ConcurrentKafkaListenerContainerFactory<Long, UserActionAvro> userActionContainerFactory() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionAvroDeserializer.class);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, userGroupId);

		ConcurrentKafkaListenerContainerFactory<Long, UserActionAvro> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));

		return factory;
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<Long, EventSimilarityAvro> eventSimilarityContainerFactory() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventSimilarityAvroDeserializer.class);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, similarityGroupId);

		ConcurrentKafkaListenerContainerFactory<Long, EventSimilarityAvro> factory = new ConcurrentKafkaListenerContainerFactory<Long, EventSimilarityAvro>();
		factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(props));

		return factory;
	}
}
