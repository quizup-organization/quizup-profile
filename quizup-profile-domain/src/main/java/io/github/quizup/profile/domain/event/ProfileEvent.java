package io.github.quizup.profile.domain.event;

import io.github.quizup.profile.domain.model.Badge;
import io.github.quizup.profile.domain.model.ProfileGame;

import java.time.Instant;

public interface ProfileEvent {
    String profileId();

    record ProfileCreatedEvent(
            String profileId,
            Instant createdAt
    ) implements ProfileEvent {
    }

    record GameResultAddedEvent(
            String profileId,
            ProfileGame game,
            Instant recordedAt
    ) implements ProfileEvent {
    }

    record ProfileExperienceIncreasedEvent(
            String profileId,
            int experienceEarned,
            Instant earnedAt
    ) implements ProfileEvent {
    }

    record ProfileGamePlayedEvent(
            String profileId,
            ProfileGame game,
            Instant playedAt
    ) implements ProfileEvent {
    }

    record ProfileWinsIncreasedEvent(
            String profileId,
            int wins,
            Instant increasedAt
    ) implements ProfileEvent {
    }

    record ProfileWinStreakIncreasedEvent(
            String profileId,
            int winStreak,
            Instant increasedAt
    ) implements ProfileEvent {
    }

    record ProfileWinStreakEndedEvent(
            String profileId,
            Instant endedAt
    ) implements ProfileEvent {
    }

    record ProfileTopicWinStreakIncreasedEvent(
            String profileId,
            String topicId,
            int winStreak,
            Instant increasedAt
    ) implements ProfileEvent {
    }

    record ProfileBadgeUnlockedEvent(
            String profileId,
            Badge badge,
            Instant unlockedAt
    ) implements ProfileEvent {
    }
}

