package ru.practicum.analyzer.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.entity.Interaction;
import ru.practicum.analyzer.entity.Similarity;
import ru.practicum.analyzer.entity.mapper.InteractionMapper;
import ru.practicum.analyzer.entity.mapper.SimilarityMapper;
import ru.practicum.analyzer.repository.InteractionRepository;
import ru.practicum.analyzer.repository.SimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnalyzerDataBaseService {
	final InteractionRepository interactionRepository;
	final SimilarityRepository similarityRepository;

	final InteractionMapper interactionMapper;
	final SimilarityMapper similarityMapper;

	@Transactional
	public void saveInteraction(UserActionAvro userActionAvro) {
		Optional<Interaction> opt = interactionRepository
				.findByUserIdAndEventId(userActionAvro.getUserId(), userActionAvro.getEventId());

		Interaction newInteraction = interactionMapper.toInteraction(userActionAvro);
		if (opt.isEmpty()) {
			interactionRepository.save(newInteraction);
			return;
		}
		Interaction fromDb = opt.get();

		double interactionFromDb = fromDb.getRating();
		double currentInteraction = newInteraction.getRating();

		if (currentInteraction > interactionFromDb) {
			fromDb.setTs(newInteraction.getTs());
			fromDb.setRating(newInteraction.getRating());
			interactionRepository.save(fromDb);
		}
	}

	@Transactional
	public void saveSimilarity(EventSimilarityAvro similarityAvro) {
		Optional<Similarity> opt = similarityRepository
				.findByEvent1AndEvent2(similarityAvro.getEventA(), similarityAvro.getEventB());

		if (opt.isEmpty()) {
			similarityRepository.save(similarityMapper.toSimilarity(similarityAvro));
			return;
		}

		Similarity similarity = opt.get();
		similarity.setSimilarity(similarityAvro.getScore());
		similarity.setTs(similarityAvro.getTimestamp());
		similarityRepository.save(similarity);
	}
}
