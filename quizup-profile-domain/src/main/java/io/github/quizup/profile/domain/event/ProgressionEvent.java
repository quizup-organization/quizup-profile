package io.github.quizup.profile.domain.event;

import io.github.quizup.profile.domain.model.Badge;
import io.github.quizup.profile.domain.model.MasteryGrade;

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
            /** Bonnes réponses sur les questions illustrées (badge « Œil de lynx »). */
            int imageCorrectAnswers,
            Instant awardedAt,
            /** Elo courant après le duel (inchangé si le duel n'est pas classé). */
            int rating,
            /** Meilleur Elo atteint. */
            int peakRating,
            /** Nombre de duels classés cumulés. */
            int ratedGames,
            boolean provisional,
            /** Variation d'Elo du duel (0 si non classé ou plafond anti-farm atteint). */
            int ratingDelta,
            /** Adversaire classé du duel, `null` si non classé. */
            String ratedOpponentId,
            /** Jour local du duel classé, `null` si non classé. */
            java.time.LocalDate ratedDay
    ) implements ProgressionEvent {
    }

    /**
     * Palier de niveau franchi.
     */
    /**
     * Grade de **maîtrise d'un sujet** franchi (titre thématique débloqué) : permet de célébrer
     * l'acquisition côté joueur, comme {@link BadgeEarnedEvent} et {@link LevelReachedEvent}.
     */
    record MasteryGradeReachedEvent(
            String userId,
            String topicId,
            MasteryGrade grade,
            Instant reachedAt
    ) implements ProgressionEvent {
    }

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
    /** Compteur hors duel enregistré (abonné gagné, défi lancé) avant évaluation. */
    record BadgeCounterRecordedEvent(
            String badgeId,
            String userId,
            io.github.quizup.profile.domain.model.BadgeMetric metric,
            String sourceId,
            Instant recordedAt
    ) implements ProgressionEvent {
    }

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
            int imageCorrectAnswers,
            Instant recordedAt,
            /** Jour local du duel (fuseau d'activité) : alimente la série de jours joués. */
            java.time.LocalDate activeDay
    ) implements ProgressionEvent {
    }
}
