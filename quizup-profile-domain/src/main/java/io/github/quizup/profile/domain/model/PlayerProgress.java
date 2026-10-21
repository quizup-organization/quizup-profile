package io.github.quizup.profile.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * Modèle domaine (read model) de la progression d'un joueur :
 * XP totale + XP par thème, parties humaines par thème, niveau, titre et badges.
 */
@Builder(toBuilder = true)
public record PlayerProgress(
        String userId,
        int xpTotal,
        int level,
        String title,
        Map<String, Integer> xpByTopic,
        Map<String, Integer> gamesByTopic,
        Set<Badge> badges,
        int gamesPlayed,
        int wins,
        int losses,
        int draws,
        int bestScore,
        int currentWinStreak,
        int bestWinStreak,
        int perfectDuels,
        int fastAnswersTotal,
        int followersGained,
        int challengesSent,
        int imageCorrectTotal,
        int currentDayStreak,
        int bestDayStreak,
        LocalDate lastActiveDate,
        Instant updatedAt,
        /** Elo global courant (profil, matchmaking, bordure). */
        int rating,
        /** Meilleur Elo atteint (trophée de carrière). */
        int peakRating,
        /** Nombre de duels classés (hors bot, hors plafond anti-farm). */
        int ratedGames,
        /** Elo encore provisoire (moins de {@link EloRules#PROVISIONAL_GAMES} duels classés). */
        boolean provisional
) {

    public PlayerProgress {
        // Les constructions historiques (tests, snapshots) ne renseignent pas encore l'Elo :
        // une progression sans aucun duel classé part de l'échelle de départ.
        if (rating == 0 && peakRating == 0 && ratedGames == 0) {
            rating = EloRules.INITIAL_RATING;
            peakRating = EloRules.INITIAL_RATING;
        }
        rating = Math.max(0, rating);
        peakRating = Math.max(peakRating, rating);
        ratedGames = Math.max(0, ratedGames);
        provisional = EloRules.isProvisional(ratedGames);
    }

    /**
     * Progression neutre (niveau 1, 0 XP, Elo de départ) d'un joueur n'ayant encore joué aucun
     * duel.
     */
    public static PlayerProgress empty(String userId) {
        return PlayerProgress.builder()
                .userId(userId)
                .xpTotal(0)
                .level(1)
                .title(ProgressionRules.titleFor(1))
                .xpByTopic(Map.of())
                .gamesByTopic(Map.of())
                .badges(Set.of())
                .gamesPlayed(0)
                .wins(0)
                .losses(0)
                .draws(0)
                .bestScore(0)
                .currentWinStreak(0)
                .bestWinStreak(0)
                .perfectDuels(0)
                .fastAnswersTotal(0)
                .followersGained(0)
                .challengesSent(0)
                .imageCorrectTotal(0)
                .currentDayStreak(0)
                .bestDayStreak(0)
                .lastActiveDate(null)
                .updatedAt(Instant.now())
                .rating(EloRules.INITIAL_RATING)
                .peakRating(EloRules.INITIAL_RATING)
                .ratedGames(0)
                .provisional(true)
                .build();
    }

    public int xpForTopic(String topicId) {
        return xpByTopic.getOrDefault(topicId, 0);
    }

    /** Parties humaines jouées sur un thème (duels bot exclus). */
    public int gamesForTopic(String topicId) {
        return gamesByTopic.getOrDefault(topicId, 0);
    }

    public int xpForNextLevel() {
        return ProgressionRules.xpForNextLevel(level);
    }
}
