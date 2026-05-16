package deserializer;

import org.apache.kafka.common.serialization.Deserializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;

public class UserActionAvroDeserializer implements Deserializer<UserActionAvro> {
	private final BaseAvroDeserializer<UserActionAvro> deserializer = new BaseAvroDeserializer<>(UserActionAvro.getClassSchema());

	@Override
	public UserActionAvro deserialize(String s, byte[] bytes) {
		return deserializer.deserialize(s, bytes);
	}
}
