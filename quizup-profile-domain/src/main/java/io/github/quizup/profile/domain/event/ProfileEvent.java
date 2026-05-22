package io.github.quizup.profile.domain.event;

import io.github.quizup.profile.domain.model.BadgeType;
import io.github.quizup.profile.domain.model.GameResultType;

import java.time.Instant;
import java.util.Set;

public interface ProfileEvent {
    String profileId();

    record ProfileCreatedEvent(
            String profileId,
            Instant createdAt
    ) implements ProfileEvent {
    }

    record GameResultRecordedEvent(
            String profileId,
            String gameId,
            String topicId,
            String opponentId,
            int playerScore,
            int opponentScore,
            GameResultType result,
            int xpEarned,
            int newGlobalTotalExperience,
            int newGlobalWins,
            int newGlobalLosses,
            int newGlobalDraws,
            int newWinStreak,
            int newLossStreak,
            int newDrawStreak,
            int newTopicTotalExperience,
            int newTopicWins,
            int newTopicLosses,
            int newTopicDraws,
            int newTopicWinStreak,
            Set<BadgeType> newBadges,
            boolean leveledUp,
            Instant recordedAt
    ) implements ProfileEvent {
    }
}

