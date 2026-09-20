package io.github.quizup.profile.domain.port.out;

/**
 * Port sortant des KPI métier de progression (profil, XP, niveaux, badges).
 *
 * <p>Implémenté en infrastructure avec Micrometer. Types JDK uniquement (règle hexagonale).
 */
public interface ProgressionMetricsPort {

    void profileCreated();

    void profileUpdated();

    /**
     * XP attribuée après une partie.
     *
     * @param topicId thème joué
     * @param xp      XP gagnée
     * @param won     victoire
     */
    void xpAwarded(String topicId, int xp, boolean won);

    /** Un palier de niveau a été atteint. */
    void levelReached(int level);

    /** Un badge a été obtenu ({@code badge} = nom de l'énumération). */
    void badgeEarned(String badge);
}
