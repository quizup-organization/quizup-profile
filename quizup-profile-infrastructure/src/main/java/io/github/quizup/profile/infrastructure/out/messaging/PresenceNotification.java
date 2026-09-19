package io.github.quizup.profile.infrastructure.out.messaging;

import io.github.quizup.profile.domain.model.PresenceStatus;

import java.time.Instant;

/**
 * Notification temps réel de présence, diffusée sur {@code /topic/presence/{userId}}.
 */
public record PresenceNotification(
        String userId,
        PresenceStatus status,
        Instant lastSeenAt
) {
}
