package io.github.quizup.profile.domain.model;

public record ProfileStreak(
        int winStreak,
        int lossStreak,
        int drawStreak
) implements Streak {

    public static ProfileStreak of(int winStreak, int lossStreak, int drawStreak) {
        return new ProfileStreak(winStreak, lossStreak, drawStreak);
    }
}
