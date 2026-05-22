package io.github.quizup.profile.domain.model;


public record GlobalStatistics(
        int totalExperience,
        int wins,
        int losses,
        int draws
) implements Statistics {


    public static GlobalStatistics empty() {
        return new GlobalStatistics(0, 0, 0, 0);
    }

}
