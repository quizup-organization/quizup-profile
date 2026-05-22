package io.github.quizup.profile.domain.event;

import java.time.Instant;

public interface ProfileEvent {
    String profileId();

    record ProfileCreatedEvent(
            String profileId,
            String userId,
            Instant createdAt
    ) implements ProfileEvent {
    }
}

