package deserializer;

import org.apache.avro.Schema;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;

public class BaseAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {
	private final DecoderFactory decoderFactory = DecoderFactory.get();
	private final Schema schema;

	public BaseAvroDeserializer(Schema schema) {
		this.schema = schema;
	}

	@Override
	public T deserialize(String s, byte[] bytes) {
		if (bytes == null) return null;

		try {
			SpecificDatumReader<T> reader = new SpecificDatumReader<>(schema);

			return reader.read(null, decoderFactory.binaryDecoder(bytes, null));
		} catch (IOException e) {
			throw new RuntimeException("Ошибка десериализации avro: " + e);
		}
	}
}
