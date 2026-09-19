package io.github.quizup.profile.infrastructure.in.api.response;

import java.io.Serializable;

/**
 * DTO d'un badge débloqué.
 */
public record BadgeResponse(
        String code,
        String label
) implements Serializable {
}
