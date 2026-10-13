package io.github.quizup.profile.domain.query;

import java.util.List;

public interface ProgressionQuery {

    record GetProgressionQuery(String userId) implements ProgressionQuery {
    }

    /**
     * Résolution en lot des progressions (évite le fan-out N+1, ex. tri par niveau).
     */
    record GetProgressionsByIdsQuery(List<String> userIds) implements ProgressionQuery {
    }

    record GetTopicProgressionQuery(String userId, String topicId) implements ProgressionQuery {
    }

    /**
     * Répartition de la progression par thème (XP, niveau et parties humaines), triée par
     * parties décroissantes — alimente le donut « Sujets joués » du profil.
     */
    record GetTopicProgressionsQuery(String userId) implements ProgressionQuery {
    }

    /**
     * XP attribuée pour une liste de parties (écran de résultat / historique).
     */
    record GetGamesXpQuery(String userId, List<String> gameIds) implements ProgressionQuery {
    }
}
