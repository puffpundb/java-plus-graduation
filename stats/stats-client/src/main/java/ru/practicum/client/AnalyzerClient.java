package ru.practicum.client;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.*;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class AnalyzerClient {
	@GrpcClient("analyzer")
	private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub;

	public Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
		UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
				.setUserId(userId)
				.setMaxResults(maxResults)
				.build();
		Iterator<RecommendedEventProto> iterator = analyzerStub.getRecommendationsForUser(request);
		return asStream(iterator);
	}

	public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
		SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
				.setEventId(eventId)
				.setUserId(userId)
				.setMaxResults(maxResults)
				.build();
		Iterator<RecommendedEventProto> iterator = analyzerStub.getSimilarEvents(request);
		return asStream(iterator);
	}

	public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
		InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
				.addAllEventId(eventIds)
				.build();
		Iterator<RecommendedEventProto> iterator = analyzerStub.getInteractionsCount(request);
		return asStream(iterator);
	}

	public double getRatingForEvent(long eventId) {
		return getInteractionsCount(List.of(eventId))
				.findFirst()
				.map(RecommendedEventProto::getScore)
				.orElse(0.0);
	}

	private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
		return StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
				false
		);
	}
}
