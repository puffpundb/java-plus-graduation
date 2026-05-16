package deserializer;

import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public class EventSimilarityAvroDeserializer implements Deserializer<EventSimilarityAvro> {
	private final BaseAvroDeserializer<EventSimilarityAvro> deserializer = new BaseAvroDeserializer<>(EventSimilarityAvro.getClassSchema());

	@Override
	public EventSimilarityAvro deserialize(String s, byte[] bytes) {
		return deserializer.deserialize(s, bytes);
	}
}
