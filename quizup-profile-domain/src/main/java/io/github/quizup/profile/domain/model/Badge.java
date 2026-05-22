package io.github.quizup.profile.domain.model;

import java.time.Instant;

public record Badge(
        BadgeType type,
        Instant unlockedAt
) {
}
