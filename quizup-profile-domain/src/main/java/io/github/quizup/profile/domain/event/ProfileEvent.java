package io.github.quizup.profile.domain.event;

import io.github.quizup.profile.domain.model.ProfileMatchResult;

import java.time.Instant;

public interface ProfileEvent {
    String profileId();

    record ProfileCreatedEvent(
            String profileId,
            String userId,
            Instant createdAt
    ) implements ProfileEvent {
    }

    record ProfileMatchResultRecordedEvent(
            String profileId,
            String topicId,
            ProfileMatchResult result,
            int totalGamesPlayed,
            int totalWins,
            int totalLosses,
            int totalDraws,
            double winLossRatio,
            Instant recordedAt
    ) implements ProfileEvent {
    }
}

