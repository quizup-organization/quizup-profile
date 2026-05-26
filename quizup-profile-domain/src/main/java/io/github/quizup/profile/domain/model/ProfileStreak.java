package io.github.quizup.profile.domain.model;

public record ProfileStreak(
        int winStreak,
        int lossStreak,
        int drawStreak
) implements Streak {
}
