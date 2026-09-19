package io.github.quizup.profile.application.handler.query;

import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.TopicProgress;
import io.github.quizup.profile.domain.port.out.ProgressionRepositoryPort;
import io.github.quizup.profile.domain.query.ProgressionQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

/**
 * Handler Axon — queries de progression. Retourne une progression neutre
 * (niveau 1, 0 XP) pour un joueur n'ayant encore joué aucun duel.
 */
@Component
public class ProgressionQueryHandler {

    private final ProgressionRepositoryPort progressionRepositoryPort;

    public ProgressionQueryHandler(ProgressionRepositoryPort progressionRepositoryPort) {
        this.progressionRepositoryPort = progressionRepositoryPort;
    }

    @QueryHandler
    public PlayerProgress handle(ProgressionQuery.GetProgressionQuery query) {
        return progressionRepositoryPort.findById(query.userId())
                .orElseGet(() -> PlayerProgress.empty(query.userId()));
    }

    @QueryHandler
    public TopicProgress handle(ProgressionQuery.GetTopicProgressionQuery query) {
        int xp = progressionRepositoryPort.findById(query.userId())
                .map(progress -> progress.xpForTopic(query.topicId()))
                .orElse(0);

        return TopicProgress.of(query.topicId(), xp);
    }
}
