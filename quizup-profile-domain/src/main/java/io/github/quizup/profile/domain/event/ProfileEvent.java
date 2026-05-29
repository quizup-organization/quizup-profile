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

    record ExperienceIncreasedEvent(
            String profileId,
            int experienceEarned,
            Instant earnedAt
    ) implements ProfileEvent {
    }

    record LevelIncreasedEvent(
            String profileId,
            int level,
            Instant increasedAt
    ) implements ProfileEvent {
    }

    record WinsIncreasedEvent(
            String profileId,
            int wins,
            Instant increasedAt
    ) implements ProfileEvent {
    }

    record LossesIncreasedEvent(
            String profileId,
            int losses,
            Instant increasedAt
    ) implements ProfileEvent {
    }

    record DrawsIncreasedEvent(
            String profileId,
            int draws,
            Instant increasedAt
    ) implements ProfileEvent {
    }

    record WinStreakIncreasedEvent(
            String profileId,
            int winStreak,
            Instant increasedAt
    ) implements ProfileEvent {
    }

    record WinStreakEndedEvent(
            String profileId,
            Instant endedAt
    ) implements ProfileEvent {
    }

    record LossStreakIncreasedEvent(
            String profileId,
            int lossStreak,
            Instant increasedAt
    ) implements ProfileEvent {
    }

    record LossStreakEndedEvent(
            String profileId,
            Instant endedAt
    ) implements ProfileEvent {
    }

    record DrawStreakIncreasedEvent(
            String profileId,
            int drawStreak,
            Instant increasedAt
    ) implements ProfileEvent {
    }

    record DrawStreakEndedEvent(
            String profileId,
            Instant endedAt
    ) implements ProfileEvent {
    }

    record GamePlayedEvent(
            String profileId,
            ProfileGame game,
            Instant playedAt
    ) implements ProfileEvent {
    }

    // GameResultAddedEvent supprimé — remplacé par des événements granulaires (event sourcing pur)

    record BadgeUnlockedEvent(
            String profileId,
            Badge badge,
            Instant unlockedAt
    ) implements ProfileEvent {
    }
}

