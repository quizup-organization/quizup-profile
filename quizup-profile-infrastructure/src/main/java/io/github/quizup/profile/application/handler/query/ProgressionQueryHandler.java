package io.github.quizup.profile.application.handler.query;

import io.github.quizup.profile.domain.model.GameXp;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.TopicProgress;
import io.github.quizup.profile.domain.port.out.ProgressionAwardedGameRepositoryPort;
import io.github.quizup.profile.domain.port.out.ProgressionRepositoryPort;
import io.github.quizup.profile.domain.query.ProgressionQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handler Axon — queries de progression. Retourne une progression neutre
 * (niveau 1, 0 XP) pour un joueur n'ayant encore joué aucun duel.
 */
@Component
public class ProgressionQueryHandler {

    private final ProgressionRepositoryPort progressionRepositoryPort;
    private final ProgressionAwardedGameRepositoryPort awardedGameRepositoryPort;

    public ProgressionQueryHandler(ProgressionRepositoryPort progressionRepositoryPort,
                                   ProgressionAwardedGameRepositoryPort awardedGameRepositoryPort) {
        this.progressionRepositoryPort = progressionRepositoryPort;
        this.awardedGameRepositoryPort = awardedGameRepositoryPort;
    }

    @QueryHandler
    public PlayerProgress handle(ProgressionQuery.GetProgressionQuery query) {
        return progressionRepositoryPort.findById(query.userId())
                .orElseGet(() -> PlayerProgress.empty(query.userId()));
    }

    @QueryHandler
    public List<PlayerProgress> handle(ProgressionQuery.GetProgressionsByIdsQuery query) {
        Map<String, PlayerProgress> byId = progressionRepositoryPort.findByIds(query.userIds()).stream()
                .collect(Collectors.toMap(PlayerProgress::userId, Function.identity(), (a, _) -> a));
        return query.userIds().stream()
                .map(userId -> byId.getOrDefault(userId, PlayerProgress.empty(userId)))
                .toList();
    }

    @QueryHandler
    public TopicProgress handle(ProgressionQuery.GetTopicProgressionQuery query) {
        PlayerProgress progress = progressionRepositoryPort.findById(query.userId())
                .orElseGet(() -> PlayerProgress.empty(query.userId()));

        return TopicProgress.of(query.topicId(),
                progress.xpForTopic(query.topicId()),
                progress.gamesForTopic(query.topicId()));
    }

    /**
     * Répartition par thème (XP, niveau et parties humaines), triée par parties décroissantes
     * puis par XP décroissante — les thèmes joués uniquement contre bot (0 partie) restent
     * présents pour leur XP.
     */
    @QueryHandler
    public List<TopicProgress> handle(ProgressionQuery.GetTopicProgressionsQuery query) {
        PlayerProgress progress = progressionRepositoryPort.findById(query.userId())
                .orElseGet(() -> PlayerProgress.empty(query.userId()));

        Set<String> topicIds = new LinkedHashSet<>(progress.xpByTopic().keySet());
        topicIds.addAll(progress.gamesByTopic().keySet());

        return topicIds.stream()
                .map(topicId -> TopicProgress.of(topicId,
                        progress.xpForTopic(topicId),
                        progress.gamesForTopic(topicId)))
                .sorted(Comparator.comparingInt(TopicProgress::games).reversed()
                        .thenComparing(Comparator.comparingInt(TopicProgress::xp).reversed())
                        .thenComparing(TopicProgress::topicId))
                .toList();
    }

    @QueryHandler
    public List<GameXp> handle(ProgressionQuery.GetGamesXpQuery query) {
        return awardedGameRepositoryPort.findGameXp(query.userId(), query.gameIds());
    }
}
