package io.github.quizup.profile.domain.model;

/**
 * Badge (exploit) débloqué par un joueur.
 */
public enum Badge {

    /** Gagner son premier duel. */
    FIRST_WIN("Première victoire"),

    /** Terminer un duel avec le score maximum (160/160). */
    PERFECT("Perfectionniste"),

    /** Répondre correctement à 5 questions en moins de 3 secondes chacune. */
    LIGHTNING("Éclair"),

    /** Enchaîner 10 victoires consécutives dans un même thème. */
    STREAK_MASTER("Série de feu");

    private final String label;

    Badge(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
