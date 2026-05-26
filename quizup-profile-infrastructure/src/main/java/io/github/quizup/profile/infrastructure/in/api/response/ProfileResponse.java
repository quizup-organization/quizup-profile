package io.github.quizup.profile.infrastructure.in.api.response;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ProfileResponse(
        String profileId,
        int totalExperience,
        int level,
        int experience,
        int experienceAtCurrentLevel,
        int experienceAtNextLevel,
        int experienceRequiredToCompleteCurrentLevel,
        int wins,
        int losses,
        int draws,
        int totalGames,
        int winPercentage,
        int lossPercentage,
        int drawPercentage,
        int winStreak,
        int lossStreak,
        int drawStreak,
        Map<String, ProfileStatisticsResponse> topics,
        List<GameResultResponse> games,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
}

