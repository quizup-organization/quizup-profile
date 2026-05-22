package io.github.quizup.profile.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Builder(toBuilder = true)
public record Profile(
        String profileId,
        int winStreak,
        int lossStreak,
        int drawStreak,
        GlobalStatistics globalStatistics,
        Map<String, TopicStatistics> topicStatistics,
        Map<BadgeType, Badge> badges,
        List<GameResult> recentGameResults,
        Instant createdAt,
        Instant updatedAt
) {

    public static Profile empty(String profileId, Instant now) {
        return new Profile(
                profileId,
                0,
                0,
                0,
                GlobalStatistics.empty(),
                Map.of(),
                Map.of(),
                List.of(),
                now,
                now
        );
    }
}

