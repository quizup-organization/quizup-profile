package io.github.quizup.profile.domain.query;

import java.util.List;

public interface ProgressionQuery {

    record GetProgressionQuery(String userId) implements ProgressionQuery {
    }

    /**
     * Résolution en lot des progressions (évite le fan-out N+1, ex. tri par niveau).
     */
    record GetProgressionsByIdsQuery(List<String> userIds) implements ProgressionQuery {
    }

    record GetTopicProgressionQuery(String userId, String topicId) implements ProgressionQuery {
    }
}
