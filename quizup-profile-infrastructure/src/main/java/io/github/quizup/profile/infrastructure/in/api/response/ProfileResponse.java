package io.github.quizup.profile.infrastructure.in.api.response;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProfileResponse(
        String profileId,
        int winStreak,
        int lossStreak,
        int drawStreak,
        ProfileStatisticsResponse globalStatistics,
        Map<String, ProfileStatisticsResponse> topicStatistics,
        List<BadgeResponse> badges,
        List<GameResultResponse> recentGameResults,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}

