package io.github.quizup.profile.domain.model;

public final class ProfileRules {

    public static final int MAX_RECENT_GAMES = 10;
    public static final int GAME_BONUS = 40;
    public static final int VICTORY_BONUS = 50;
    private static final int LEVEL_FACTOR = 25;

    private ProfileRules() {}



    /**
     * XP totale nécessaire pour atteindre le début du niveau {@code level}.
     * Niveau 0 → 0 XP. Niveau 1 → 100 XP. etc.
     */
    public static int computeXpForLevel(int level) {
        return (int) ((Math.pow(level, 2) + 3.0 * level) * LEVEL_FACTOR);
    }


    /**
     * Niveau correspondant à {@code totalXp} XP accumulés.
     */
    public static int computeLevelFromXp(int totalExperience) {
        return (int) ((-3 + Math.sqrt(9 + 4.0 * totalExperience / LEVEL_FACTOR)) / 2);
    }

    /**
     * Calcule l'XP gagnée pour une partie.
     * = score_partie + GAME_BONUS [+ VICTORY_BONUS si victoire]
     */
    public static int computeXpEarned(int gameScore, GameResultType result) {
        return gameScore + GAME_BONUS + (result == GameResultType.WIN ? VICTORY_BONUS : 0);
    }
}