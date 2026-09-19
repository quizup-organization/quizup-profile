package io.github.quizup.profile.infrastructure.in.api.response;

import io.github.quizup.profile.domain.model.PresenceStatus;

import java.time.Instant;

/**
 * DTO de réponse pour la présence d'un joueur.
 */
public record PresenceResponse(
        String userId,
        PresenceStatus status,
        Instant lastSeenAt
) {
}
