package io.github.quizup.profile.domain.event;

import io.github.quizup.profile.domain.model.Badge;

import java.time.Instant;

public interface ProgressionEvent {

    String userId();

    /**
     * XP attribuée pour un duel (un événement par joueur et par duel).
     */
    record XpAwardedEvent(
            String userId,
            String gameId,
            String topicId,
            int xp,
            int gameScore,
            boolean won,
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
}
