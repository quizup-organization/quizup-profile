package io.github.quizup.profile.infrastructure.in.api.response;

import java.io.Serializable;
import java.time.Instant;

public record BadgeResponse(
        BadgeType type,
        Instant unlockedAt
) implements Serializable {
}

