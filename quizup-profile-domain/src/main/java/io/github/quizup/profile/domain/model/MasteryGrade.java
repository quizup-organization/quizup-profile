package io.github.quizup.profile.domain.model;

import java.util.Optional;

/**
 * Grade de **maîtrise d'un sujet** : échelle distincte du niveau de compte, portée par l'XP
 * cumulée **dans le sujet**. C'est le support des **titres thématiques** (« Historien
 * confirmé ») : le titre affiché est un grade + le nom du sujet (cf. {@link PlayerTitle}).
 *
 * <p>Table de seuils <b>explicite</b> (XP dans le sujet) — volontairement plus raide que la
 * courbe de compte : une maîtrise s'entretient sur un domaine, elle ne se dilue pas.</p>
 *
 * <pre>
 * Novice 0 · Apprenti 300 · Pratiquant 1 000 · Confirmé 2 500
 * Expert 6 000 · Maître 14 000 · Légende 30 000
 * </pre>
 *
 * <p>Référence à ~120 XP par duel : Confirmé ≈ 20 duels sur le sujet, Expert ≈ 50, Légende
 * ≈ 250.</p>
 */
public enum MasteryGrade {

    NOVICE("Novice", 0),
    APPRENTI("Apprenti", 300),
    PRATIQUANT("Pratiquant", 1_000),
    CONFIRME("Confirmé", 2_500),
    EXPERT("Expert", 6_000),
    MAITRE("Maître", 14_000),
    LEGENDE("Légende", 30_000);

    private final String label;
    private final int xpThreshold;

    MasteryGrade(String label, int xpThreshold) {
        this.label = label;
        this.xpThreshold = xpThreshold;
    }

    /** Libellé affiché du grade (« Confirmé »). */
    public String label() {
        return label;
    }

    /** XP cumulée dans le sujet nécessaire pour atteindre ce grade. */
    public int xpThreshold() {
        return xpThreshold;
    }

    public boolean isLast() {
        return ordinal() == values().length - 1;
    }

    /** Grade suivant, ou {@code null} pour le dernier (Légende). */
    public MasteryGrade next() {
        return isLast() ? null : values()[ordinal() + 1];
    }

    /** Progression (0–100) vers le grade suivant ; 100 pour le dernier grade. */
    public int progressPercent(int topicXp) {
        if (isLast()) {
            return 100;
        }

        int floor = xpThreshold;
        int span = next().xpThreshold - floor;
        if (span <= 0 || topicXp <= floor) {
            return 0;
        }

        return Math.min(100, (int) ((long) (topicXp - floor) * 100 / span));
    }

    /** XP restante dans le sujet avant le grade suivant ({@code 0} pour le dernier grade). */
    public int remainingXp(int topicXp) {
        if (isLast()) {
            return 0;
        }

        return Math.max(0, next().xpThreshold - Math.max(0, topicXp));
    }

    /**
     * Grade atteint pour une XP cumulée dans un sujet. Un joueur qui n'a jamais joué le sujet est
     * {@link #NOVICE}.
     */
    public static MasteryGrade gradeFor(int topicXp) {
        MasteryGrade reached = NOVICE;
        for (MasteryGrade grade : values()) {
            if (topicXp >= grade.xpThreshold) {
                reached = grade;
            }
        }

        return reached;
    }

    /** Grade depuis son nom (clé de titre persistée), insensible à la casse. */
    public static Optional<MasteryGrade> fromKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        for (MasteryGrade grade : values()) {
            if (grade.name().equalsIgnoreCase(key.trim())) {
                return Optional.of(grade);
            }
        }

        return Optional.empty();
    }
}
