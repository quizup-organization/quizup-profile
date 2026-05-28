package io.github.quizup.profile.domain.event;

import io.github.quizup.profile.domain.model.ProfileGame;

import java.time.Instant;

public interface ProfileTopicEvent extends ProfileEvent {

    String topicId();

    record TopicExperienceIncreasedEvent(
            String profileId,
            String topicId,
            int experienceEarned,
            Instant earnedAt
    ) implements ProfileTopicEvent {
    }

    record TopicLevelIncreasedEvent(
            String profileId,
            String topicId,
            int level,
            Instant increasedAt
    ) implements ProfileTopicEvent {
    }

    record TopicWinsIncreasedEvent(
            String profileId,
            String topicId,
            int wins,
            Instant increasedAt
    ) implements ProfileTopicEvent {
    }

    record TopicLossesIncreasedEvent(
            String profileId,
            String topicId,
            int losses,
            Instant increasedAt
    ) implements ProfileTopicEvent {
    }

    record TopicDrawsIncreasedEvent(
            String profileId,
            String topicId,
            int draws,
            Instant increasedAt
    ) implements ProfileTopicEvent {
    }

    record TopicWinStreakIncreasedEvent(
            String profileId,
            String topicId,
            int winStreak,
            Instant increasedAt
    ) implements ProfileTopicEvent {
    }

    record TopicWinStreakEndedEvent(
            String profileId,
            String topicId,
            Instant endedAt
    ) implements ProfileTopicEvent {
    }

    record TopicLossStreakIncreasedEvent(
            String profileId,
            String topicId,
            int lossStreak,
            Instant increasedAt
    ) implements ProfileTopicEvent {
    }

    record TopicLossStreakEndedEvent(
            String profileId,
            String topicId,
            Instant endedAt
    ) implements ProfileTopicEvent {
    }

    record TopicDrawStreakIncreasedEvent(
            String profileId,
            String topicId,
            int drawStreak,
            Instant increasedAt
    ) implements ProfileTopicEvent {
    }

    record TopicDrawStreakEndedEvent(
            String profileId,
            String topicId,
            Instant endedAt
    ) implements ProfileTopicEvent {
    }

    record TopicGamePlayedEvent(
            String profileId,
            String topicId,
            ProfileGame game,
            Instant playedAt
    ) implements ProfileTopicEvent {

    }
}

