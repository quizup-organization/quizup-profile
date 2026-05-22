package io.github.quizup.profile.domain.model;

public interface Statistics {

    int level();

    int experience();

    default int experienceBeforeNextLevel() {
        return ProfileRules.computeXpForLevel(level() + 1);
    }

    int wins();

    int losses();

    int draws();

    default int totalGames() {
        return wins() + losses() + draws();
    }

    default int winPercentage() {
        if (totalGames() == 0) {
            return 0;
        }
        return (int) ((wins() * 100.0) / totalGames());
    }

    default int lossPercentage() {
        if (totalGames() == 0) {
            return 0;
        }
        return (int) ((losses() * 100.0) / totalGames());
    }

    default int drawPercentage() {
        if (totalGames() == 0) {
            return 0;
        }
        return (int) ((draws() * 100.0) / totalGames());
    }
}
