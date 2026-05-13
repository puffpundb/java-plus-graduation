package ru.practicum.analyzer.grpc;

import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.analyzer.entity.Interaction;
import ru.practicum.analyzer.entity.Similarity;
import ru.practicum.analyzer.repository.InteractionRepository;
import ru.practicum.analyzer.repository.SimilarityRepository;
import ru.practicum.ewm.stats.proto.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecommendationsService extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
	final InteractionRepository interactionRepository;
	final SimilarityRepository similarityRepository;

	static final int K_NEIGHBORS = 20;

	@Override
	public void getInteractionsCount(InteractionsCountRequestProto request,
									 StreamObserver<RecommendedEventProto> responseObserver) {
		try {
			for (Long eventId : request.getEventIdList()) {
				Double total = interactionRepository.sumRatingByEventId(eventId);
				responseObserver.onNext(RecommendedEventProto.newBuilder()
						.setEventId(eventId)
						.setScore(total == null ? 0.0 : total)
						.build());
			}
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.error("Error in getInteractionsCount", e);
			responseObserver.onError(e);
		}
	}

	@Override
	public void getSimilarEvents(SimilarEventsRequestProto request,
								 StreamObserver<RecommendedEventProto> responseObserver) {
		try {
			long eventId = request.getEventId();
			long userId = request.getUserId();
			int maxResults = request.getMaxResults();

			List<Similarity> similarities = similarityRepository.findByEvent1OrEvent2(eventId, eventId);
			if (similarities.isEmpty()) {
				responseObserver.onCompleted();
				return;
			}

			Set<Long> userEvents = interactionRepository.findByUserId(userId).stream()
					.map(Interaction::getEventId)
					.collect(Collectors.toSet());

			List<RecommendedEventProto> result = similarities.stream()
					.map(sim -> sim.getEvent1() == eventId ? sim.getEvent2() : sim.getEvent1())
					.filter(other -> !userEvents.contains(other))
					.map(other -> {
						double score = similarities.stream()
								.filter(s -> (s.getEvent1() == eventId && s.getEvent2() == other) ||
										(s.getEvent1() == other && s.getEvent2() == eventId))
								.findFirst()
								.map(Similarity::getSimilarity)
								.orElse(0.0);
						return RecommendedEventProto.newBuilder()
								.setEventId(other)
								.setScore(score)
								.build();
					})
					.sorted(Comparator.comparingDouble(RecommendedEventProto::getScore).reversed())
					.limit(maxResults)
					.toList();

			result.forEach(responseObserver::onNext);
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.error("Error in getSimilarEvents", e);
			responseObserver.onError(e);
		}
	}

	@Override
	public void getRecommendationsForUser(UserPredictionsRequestProto request,
										  StreamObserver<RecommendedEventProto> responseObserver) {
		try {
			long userId = request.getUserId();
			int maxResults = request.getMaxResults();

			List<Interaction> userInteractions = interactionRepository.findByUserIdOrderByTsDesc(userId);
			if (userInteractions.isEmpty()) {
				responseObserver.onCompleted();
				return;
			}
			if (userInteractions.size() > maxResults) {
				userInteractions = userInteractions.subList(0, maxResults);
			}

			Set<Long> seenEvents = userInteractions.stream()
					.map(Interaction::getEventId)
					.collect(Collectors.toSet());

			Map<Long, List<Similarity>> candidateNeighbors = new HashMap<>();
			for (Interaction interaction : userInteractions) {
				long currentId = interaction.getEventId();
				List<Similarity> sims = similarityRepository.findByEvent1OrEvent2(currentId, currentId);
				for (Similarity sim : sims) {
					long other = (sim.getEvent1() == currentId) ? sim.getEvent2() : sim.getEvent1();
					if (seenEvents.contains(other)) continue;
					candidateNeighbors.computeIfAbsent(other, k -> new ArrayList<>()).add(sim);
				}
			}

			if (candidateNeighbors.isEmpty()) {
				responseObserver.onCompleted();
				return;
			}

			List<RecommendedEventProto> recommendations = new ArrayList<>();
			for (Map.Entry<Long, List<Similarity>> entry : candidateNeighbors.entrySet()) {
				long candidateId = entry.getKey();
				List<Similarity> allNeighbors = entry.getValue();

				allNeighbors.sort((s1, s2) -> Double.compare(s2.getSimilarity(), s1.getSimilarity()));
				int k = Math.min(K_NEIGHBORS, allNeighbors.size());
				List<Similarity> topNeighbors = allNeighbors.subList(0, k);

				double weightedSum = 0.0;
				double similaritySum = 0.0;
				for (Similarity sim : topNeighbors) {
					long neighborId = (sim.getEvent1() == candidateId) ? sim.getEvent2() : sim.getEvent1();
					double rating = interactionRepository.findByUserIdAndEventId(userId, neighborId)
							.map(Interaction::getRating)
							.orElse(0.0);
					weightedSum += rating * sim.getSimilarity();
					similaritySum += sim.getSimilarity();
				}
				double predictedScore = (similaritySum == 0.0) ? 0.0 : weightedSum / similaritySum;
				recommendations.add(RecommendedEventProto.newBuilder()
						.setEventId(candidateId)
						.setScore(predictedScore)
						.build());
			}

			recommendations.stream()
					.sorted(Comparator.comparingDouble(RecommendedEventProto::getScore).reversed())
					.limit(maxResults)
					.forEach(responseObserver::onNext);
			responseObserver.onCompleted();

		} catch (Exception e) {
			log.error("Error in getRecommendationsForUser", e);
			responseObserver.onError(e);
		}
	}
}
