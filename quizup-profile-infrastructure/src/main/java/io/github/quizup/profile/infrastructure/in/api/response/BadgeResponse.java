package io.github.quizup.profile.infrastructure.in.api.response;

import io.github.quizup.profile.domain.model.BadgeType;

import java.io.Serializable;
import java.time.Instant;

public record BadgeResponse(
        BadgeType type,
        Instant unlockedAt
) implements Serializable {
}

