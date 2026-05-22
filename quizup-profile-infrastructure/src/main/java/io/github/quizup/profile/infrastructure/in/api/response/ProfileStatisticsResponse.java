package io.github.quizup.profile.infrastructure.in.api.response;

import java.io.Serializable;

public record ProfileStatisticsResponse(
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
        int drawPercentage
) implements Serializable {
}

