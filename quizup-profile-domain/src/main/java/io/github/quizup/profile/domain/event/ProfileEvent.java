package io.github.quizup.profile.domain.event;

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
}

