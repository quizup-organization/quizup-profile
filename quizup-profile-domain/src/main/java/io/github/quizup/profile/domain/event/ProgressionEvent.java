package io.github.quizup.profile.domain.event;

import io.github.quizup.profile.domain.model.Badge;

import java.time.Instant;

public interface ProgressionEvent {

    String userId();

    /**
     * XP attribuée pour un duel (un événement par joueur et par duel).
     * {@code draw} distingue l'égalité de la défaite ; {@code botGame} signale un
     * <b>entraînement</b> contre bot : {@code xp} vaut alors 0 (les badges restent évalués,
     * les stats V/N/D et le classement excluent ces duels). Les événements antérieurs à ces
     * champs sont rejoués avec {@code false} (stats legacy).
     */
    record XpAwardedEvent(
            String userId,
            String gameId,
            String topicId,
            int xp,
            int gameScore,
            boolean won,
            boolean draw,
            boolean botGame,
            int correctAnswers,
            int fastAnswers,
            Instant awardedAt
    ) implements ProgressionEvent {
    }

    /**
     * Palier de niveau franchi.
     */
    record LevelReachedEvent(
            String userId,
            int level,
            String title,
            Instant reachedAt
    ) implements ProgressionEvent {
    }

    /**
     * Badge débloqué.
     */
    record BadgeEarnedEvent(
            String userId,
            Badge badge,
            Instant earnedAt
    ) implements ProgressionEvent {
    }

    /**
     * Faits d'un duel enregistrés par l'agrégat de **badges** (compteurs mis à jour avant
     * évaluation) : duels/victoires entre joueurs, XP et série du sujet, score et réponses
     * rapides. L'entraînement contre bot n'alimente **aucun** compteur de volume/maîtrise.
     */
    record BadgeFactsRecordedEvent(
            String badgeId,
            String userId,
            String gameId,
            String topicId,
            int xp,
            int gameScore,
            boolean won,
            boolean botGame,
            int fastAnswers,
            Instant recordedAt,
            /** Jour local du duel (fuseau d'activité) : alimente la série de jours joués. */
            java.time.LocalDate activeDay
    ) implements ProgressionEvent {
    }
}
