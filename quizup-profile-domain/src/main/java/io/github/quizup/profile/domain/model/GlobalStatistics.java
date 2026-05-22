package io.github.quizup.profile.domain.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record GlobalStatistics(
        int totalExperience,
        int wins,
        int losses,
        int draws
) implements Statistics {


    public static GlobalStatistics empty() {
        return new GlobalStatistics(0, 0, 0, 0);
    }

    public GlobalStatistics addGame(int xpEarned, GameResultType result) {
        return switch (result) {
            case WIN -> new GlobalStatistics(totalExperience + xpEarned, wins + 1, losses, draws);
            case LOSS -> new GlobalStatistics(totalExperience + xpEarned, wins, losses + 1, draws);
            case DRAW -> new GlobalStatistics(totalExperience + xpEarned, wins, losses, draws + 1);
        };
    }

}
