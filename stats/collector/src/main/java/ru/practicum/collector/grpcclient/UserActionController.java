package ru.practicum.collector.grpcclient;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.collector.converter.AvroConverter;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

@GrpcService
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class UserActionController extends UserActionControllerGrpc.UserActionControllerImplBase {
	@Value("${spring.kafka.topics.user-actions}")
	String userActionTopic;

	@Autowired
	KafkaTemplate<Long, UserActionAvro> kafkaTemplate;

	@Override
	public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
		log.info("UserActionController: получен запрос на отправление действия пользователя в kafka: {}", request);
		try {
			UserActionAvro avro = AvroConverter.toUserActionAvro(request);
			kafkaTemplate.send(userActionTopic, avro.getUserId(), avro);

			responseObserver.onNext(Empty.getDefaultInstance());
			responseObserver.onCompleted();
			log.info("UserActionController: отправление успешно");
		} catch (Exception e) {
			log.info("UserActionController: Произошла ошибка");
			responseObserver.onError(e);
		}
	}
}
