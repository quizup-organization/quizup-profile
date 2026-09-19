package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.TopicProgress;
import io.github.quizup.profile.domain.query.ProgressionQuery;

import java.util.concurrent.CompletableFuture;

/**
 * Port entrant — lecture de la progression d'un joueur (globale ou par thème).
 */
public interface GetProgressionUseCase {

    CompletableFuture<PlayerProgress> getById(ProgressionQuery.GetProgressionQuery query);

    CompletableFuture<TopicProgress> getTopic(ProgressionQuery.GetTopicProgressionQuery query);

    default CompletableFuture<PlayerProgress> getById(String userId) {
        return getById(new ProgressionQuery.GetProgressionQuery(userId));
    }

    default CompletableFuture<TopicProgress> getTopic(String userId, String topicId) {
        return getTopic(new ProgressionQuery.GetTopicProgressionQuery(userId, topicId));
    }
}
