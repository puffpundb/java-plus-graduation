package ru.practicum.collector.converter;

import com.google.protobuf.util.Timestamps;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;

import static ru.practicum.ewm.stats.avro.ActionTypeAvro.*;

public class AvroConverter {
	public static UserActionAvro toUserActionAvro(UserActionProto p) {
		return UserActionAvro.newBuilder()
				.setUserId(p.getUserId())
				.setEventId(p.getEventId())
				.setActionType(convertActionType(p.getActionType()))
				.setTimestamp(Timestamps.toMillis(p.getTimestamp()))
				.build();
	}

	private static ActionTypeAvro convertActionType(ActionTypeProto p) {
		switch (p) {
			case ACTION_VIEW -> {
				return VIEW;
			}
			case ACTION_REGISTER -> {
				return REGISTER;
			}
			case ACTION_LIKE -> {
				return LIKE;
			}
			default -> throw new IllegalArgumentException("Неизвестное действие: " + p);
		}
	}
}
