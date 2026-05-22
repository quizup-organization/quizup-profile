package io.github.quizup.profile.domain.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record ProfileTopic(
        String topicId,
        String name
) {
}

