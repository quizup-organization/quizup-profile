package io.github.quizup.profile.infrastructure.in.api.response;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO pour le profil utilisateur
 */
public record ProfileResponse(
        String userId,
        String email,
        String displayName,
        String bio,
        String country,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}
