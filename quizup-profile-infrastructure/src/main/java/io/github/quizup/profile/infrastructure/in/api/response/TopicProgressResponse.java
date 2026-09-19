package io.github.quizup.profile.infrastructure.in.api.response;

import java.io.Serializable;

/**
 * DTO de progression d'un joueur dans un thème.
 */
public record TopicProgressResponse(
        String topicId,
        int xp,
        int level,
        String title
) implements Serializable {
}
