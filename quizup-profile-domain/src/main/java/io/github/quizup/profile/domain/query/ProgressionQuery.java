package io.github.quizup.profile.domain.query;

public interface ProgressionQuery {

    record GetProgressionQuery(String userId) implements ProgressionQuery {
    }

    record GetTopicProgressionQuery(String userId, String topicId) implements ProgressionQuery {
    }
}
