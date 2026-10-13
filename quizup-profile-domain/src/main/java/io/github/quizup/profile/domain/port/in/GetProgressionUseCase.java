package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.TopicProgress;
import io.github.quizup.profile.domain.query.ProgressionQuery;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Port entrant — lecture de la progression d'un joueur (globale, par thème ou par thème en lot).
 */
public interface GetProgressionUseCase {

    CompletableFuture<PlayerProgress> getById(ProgressionQuery.GetProgressionQuery query);

    CompletableFuture<TopicProgress> getTopic(ProgressionQuery.GetTopicProgressionQuery query);

    CompletableFuture<List<TopicProgress>> getTopics(ProgressionQuery.GetTopicProgressionsQuery query);

    default CompletableFuture<PlayerProgress> getById(String userId) {
        return getById(new ProgressionQuery.GetProgressionQuery(userId));
    }

    default CompletableFuture<TopicProgress> getTopic(String userId, String topicId) {
        return getTopic(new ProgressionQuery.GetTopicProgressionQuery(userId, topicId));
    }

    default CompletableFuture<List<TopicProgress>> getTopics(String userId) {
        return getTopics(new ProgressionQuery.GetTopicProgressionsQuery(userId));
    }
}
