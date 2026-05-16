package ru.practicum.client;

import com.google.protobuf.util.Timestamps;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

@Service
public class CollectorClient {
	@GrpcClient("collector")
	private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

	public void sendView(long userId, long eventId) {
		sendAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
	}

	public void sendRegister(long userId, long eventId) {
		sendAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
	}

	public void sendLike(long userId, long eventId) {
		sendAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
	}

	private void sendAction(long userId, long eventId, ActionTypeProto type) {
		UserActionProto action = UserActionProto.newBuilder()
				.setUserId(userId)
				.setEventId(eventId)
				.setActionType(type)
				.setTimestamp(Timestamps.now())
				.build();
		collectorStub.collectUserAction(action);
	}
}
