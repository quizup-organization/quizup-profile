package io.github.quizup.profile.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Modèle domaine (read model) de la progression d'un joueur :
 * XP totale + XP par thème, niveau, titre et badges.
 */
@Builder(toBuilder = true)
public record PlayerProgress(
        String userId,
        int xpTotal,
        int level,
        String title,
        Map<String, Integer> xpByTopic,
        Set<Badge> badges,
        int gamesPlayed,
        int wins,
        int losses,
        int bestScore,
        int currentWinStreak,
        int bestWinStreak,
        Instant updatedAt
) {

    /**
     * Progression neutre (niveau 1, 0 XP) d'un joueur n'ayant encore joué aucun duel.
     */
    public static PlayerProgress empty(String userId) {
        return PlayerProgress.builder()
                .userId(userId)
                .xpTotal(0)
                .level(1)
                .title(ProgressionRules.titleFor(1))
                .xpByTopic(Map.of())
                .badges(Set.of())
                .gamesPlayed(0)
                .wins(0)
                .losses(0)
                .bestScore(0)
                .currentWinStreak(0)
                .bestWinStreak(0)
                .updatedAt(Instant.now())
                .build();
    }

    public int xpForTopic(String topicId) {
        return xpByTopic.getOrDefault(topicId, 0);
    }

    public int xpForNextLevel() {
        return ProgressionRules.xpForNextLevel(level);
    }
}
