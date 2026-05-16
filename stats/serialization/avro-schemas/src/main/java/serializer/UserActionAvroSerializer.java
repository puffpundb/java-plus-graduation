package serializer;

import org.apache.kafka.common.serialization.Serializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;

public class UserActionAvroSerializer implements Serializer<UserActionAvro> {
	private final CommonSerializer common = new CommonSerializer();

	@Override
	public byte[] serialize(String s, UserActionAvro userActionAvro) {
		return common.serialize(s, userActionAvro);
	}
}
