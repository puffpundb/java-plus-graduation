package serializer;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class CommonSerializer implements Serializer<SpecificRecordBase> {
	private final EncoderFactory encoderFactory = EncoderFactory.get();

	@Override
	public void configure(Map<String, ?> configs, boolean isKey) {
		Serializer.super.configure(configs, isKey);
	}

	@Override
	public byte[] serialize(String s, SpecificRecordBase specificRecordBase) {
		if (specificRecordBase == null) return null;

		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			BinaryEncoder encoder = encoderFactory.binaryEncoder(outputStream, null);
			Schema schema = specificRecordBase.getSchema();
			SpecificDatumWriter<SpecificRecordBase> datumWriter = new SpecificDatumWriter<>(schema);

			datumWriter.write(specificRecordBase, encoder);
			encoder.flush();

			return outputStream.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("Ошибка сериализации. Topic: " + s + " исключение: " + e);
		}
	}

	@Override
	public byte[] serialize(String topic, Headers headers, SpecificRecordBase data) {
		return Serializer.super.serialize(topic, headers, data);
	}

	@Override
	public void close() {
		Serializer.super.close();
	}
}
