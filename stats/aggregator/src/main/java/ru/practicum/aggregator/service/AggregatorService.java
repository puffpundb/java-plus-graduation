package ru.practicum.aggregator.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.aggregator.kafka.SimilarityProducer;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AggregatorService {
	@Value("${aggregator.weights.view:0.4}")
	double weightView;
	@Value("${aggregator.weights.register:0.8}")
	double weightRegister;
	@Value("${aggregator.weights.like:1.0}")
	double weightLike;

	final SimilarityProducer similarityProducer;

	// userId (eventId : максимальный вес действий пользователя по отношению к событию)
	final Map<Long, Map<Long, Double>> userMaxWeights = new ConcurrentHashMap<>();

	// fEventId (sEventId : сумма минимальных весов действий пользователей)
	final Map<Long, Map<Long, Double>> minSums = new ConcurrentHashMap<>();

	// eventId : сумма квадратов максимальных весов по отношению к конкретному событию
	final Map<Long, Double> eventSums = new ConcurrentHashMap<>();

	private double getWeight(ActionTypeAvro typeAvro) {
		return switch (typeAvro) {
			case VIEW -> weightView;
			case REGISTER -> weightRegister;
			case LIKE -> weightLike;
		};
	}

	public void processUserAction(UserActionAvro actionAvro) {
		Long userId = actionAvro.getUserId();
		Long eventId = actionAvro.getEventId();
		Double newWeight = getWeight(actionAvro.getActionType());

		log.info("AggregatorService: Обработка действия пользователя: userId={}, eventId={}, type={}, weight={}",
				userId, eventId, actionAvro.getActionType(), newWeight);

		Map<Long, Double> userEvents = userMaxWeights.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());

		Double oldWeightForCurrentEvent = userEvents.getOrDefault(eventId, 0.0);

		if (newWeight <= oldWeightForCurrentEvent) {
			log.info("AggregatorService: Пропускаем новый вес <= старому ({} <= {}) для userId={}, eventId={}",
					newWeight, oldWeightForCurrentEvent, userId, eventId);
			return;
		}

		double deltaSum = newWeight - oldWeightForCurrentEvent;
		eventSums.merge(eventId, deltaSum, Double::sum);
		log.debug("eventSums[{}] += {} -> {}", eventId, deltaSum, eventSums.get(eventId));

		for (Map.Entry<Long, Double> entry: userEvents.entrySet()) {
			Long otherId = entry.getKey();
			if (Objects.equals(otherId, eventId)) continue;
			Double otherWeight = entry.getValue();

			Double oldMinWeight = Math.min(oldWeightForCurrentEvent, otherWeight);
			Double newMinWeight = Math.min(newWeight, otherWeight);
			Double deltaMin = newMinWeight - oldMinWeight;

			Long first = Math.min(eventId, otherId);
			Long second = Math.max(eventId, otherId);

			Map<Long, Double> inner = minSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>());
			inner.merge(second, deltaMin, Double::sum);

			Double minSum = inner.get(second);
			double sumA = eventSums.getOrDefault(first, 0.0);
			double sumB = eventSums.getOrDefault(second, 0.0);
			double similarity = Calculator.calculateSimilarity(minSum, sumA, sumB);

			log.info("Обновляем пару ({},{}): deltaMin={}, новая сумма минимумов={}, сходство={}",
					first, second, deltaMin, minSum, similarity);

			EventSimilarityAvro message = EventSimilarityAvro.newBuilder()
					.setEventA(first)
					.setEventB(second)
					.setScore(similarity)
					.setTimestamp(actionAvro.getTimestamp())
					.build();
			similarityProducer.send(message);
		}

		userEvents.put(eventId, newWeight);
	}
}
