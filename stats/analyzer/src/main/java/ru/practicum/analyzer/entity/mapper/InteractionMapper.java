package ru.practicum.analyzer.entity.mapper;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.entity.Interaction;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InteractionMapper {
	@Value("${analyzer.weights.view}")
	double weightView;
	@Value("${analyzer.weights.register}")
	double weightRegister;
	@Value("${analyzer.weights.like}")
	double weightLike;

	public Interaction toInteraction(UserActionAvro avro) {
		return Interaction.builder()
				.userId(avro.getUserId())
				.eventId(avro.getEventId())
				.rating(getWeight(avro.getActionType()))
				.ts(avro.getTimestamp())
				.build();
	}

	private double getWeight(ActionTypeAvro avro) {
		switch (avro) {
			case VIEW -> {
				return weightView;
			}
			case REGISTER -> {
				return weightRegister;
			}
			case LIKE -> {
				return weightLike;
			}
			default -> throw new IllegalArgumentException("Неизвестный тип действия");
		}
	}
}
