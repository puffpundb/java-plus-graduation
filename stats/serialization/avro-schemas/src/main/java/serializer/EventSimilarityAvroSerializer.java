package serializer;

import org.apache.kafka.common.serialization.Serializer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public class EventSimilarityAvroSerializer implements Serializer<EventSimilarityAvro> {
	private final CommonSerializer common = new CommonSerializer();

	@Override
	public byte[] serialize(String s, EventSimilarityAvro eventSimilarityAvro) {
		return common.serialize(s, eventSimilarityAvro);
	}
}
