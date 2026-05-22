package io.github.quizup.profile.domain.event;

import io.github.quizup.profile.domain.model.GameResult;
import io.github.quizup.profile.domain.model.Profile;

import java.time.Instant;

public interface ProfileEvent {
    String profileId();

    record ProfileCreatedEvent(
            String profileId,
            Profile profile,
            Instant createdAt
    ) implements ProfileEvent {
    }

    record GameResultRecordedEvent(
            String profileId,
            GameResult gameResult,
            Profile profile,
            Instant recordedAt
    ) implements ProfileEvent {
    }
}

