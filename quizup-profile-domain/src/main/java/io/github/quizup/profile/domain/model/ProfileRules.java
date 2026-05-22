package io.github.quizup.profile.domain.model;

public final class ProfileRules {

    public static final int GAME_BONUS = 40;
    public static final int VICTORY_BONUS = 50;
    private static final int LEVEL_FACTOR = 25;

    private ProfileRules() {}

    public static int computeXpForLevel(int level) {
        return (int) ((Math.pow(level, 2) + 3.0 * level) * LEVEL_FACTOR);
    }

    public static int computeLevelFromXp(int totalXp) {
        return (int) ((-3 + Math.sqrt(9 + 4.0 * totalXp / LEVEL_FACTOR)) / 2);
    }
}