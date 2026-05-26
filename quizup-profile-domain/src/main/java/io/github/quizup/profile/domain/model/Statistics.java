package io.github.quizup.profile.domain.model;

import java.util.List;

public interface Statistics extends Streak {

    default int level() {
        return ProfileRules.computeLevelFromXp(totalExperience());
    }

    default int experience() {
        return experienceInCurrentLevel();
    }

    int totalExperience();

    /**
     * XP totale requise pour ENTRER dans le niveau actuel.
     * Exemple : si level=5, retourne le seuil d'entrée du niveau 5.
     */
    default int experienceAtCurrentLevel() {
        return ProfileRules.computeXpForLevel(level());
    }

    /**
     * XP totale requise pour ENTRER dans le niveau suivant.
     * Exemple : si level=5, retourne le seuil d'entrée du niveau 6.
     */
    default int experienceAtNextLevel() {
        return ProfileRules.computeXpForLevel(level() + 1);
    }

    /**
     * XP gagnée dans le niveau actuel (pour la barre de progression UI).
     */
    default int experienceInCurrentLevel() {
        return totalExperience() - experienceAtCurrentLevel();
    }

    /**
     * XP totale nécessaire pour compléter le niveau actuel (dénominateur de la barre).
     */
    default int experienceRequiredToCompleteCurrentLevel() {
        return experienceAtNextLevel() - experienceAtCurrentLevel();
    }

    int wins();

    int losses();

    int draws();

    List<ProfileGame> games();

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
