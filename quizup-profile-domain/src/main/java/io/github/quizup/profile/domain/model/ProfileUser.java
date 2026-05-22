package io.github.quizup.profile.domain.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record ProfileUser(
        String userId,
        String name
) {
}

