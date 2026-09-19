package io.github.quizup.profile.domain.model;

/**
 * Règles de progression (pures, sans dépendance framework) : calcul de l'XP
 * gagnée, du niveau atteint et du titre associé.
 *
 * <p>Barème : XP = score du match + bonus de victoire. Le niveau suit une
 * progression quadratique simple : {@code niveau = 1 + floor(sqrt(xp / 100))}.</p>
 */
public final class ProgressionRules {

    /** Score maximal d'un duel (6 questions normales + 1 bonus) — cf. spec produit. */
    public static final int PERFECT_SCORE = 160;

    /** Bonus d'XP accordé pour une victoire. */
    public static final int WIN_BONUS = 50;

    /** Unité d'XP par palier de niveau. */
    private static final int XP_PER_LEVEL_UNIT = 100;

    private ProgressionRules() {
    }

    /**
     * Identifiant d'agrégat de progression pour un utilisateur.
     *
     * <p>Namespacé volontairement : Axon lit les événements d'un agrégat par
     * identifiant unique, sans le type d'agrégat. Le {@code userId} étant déjà
     * l'identifiant du {@code ProfileAggregate}, les deux agrégats ne peuvent
     * partager le même identifiant.</p>
     */
    public static String progressIdFor(String userId) {
        return "progress:" + userId;
    }

    /**
     * XP gagnée à l'issue d'un duel.
     *
     * @param matchScore score obtenu dans le duel
     * @param won        vrai si le joueur a gagné
     * @return XP (jamais négative)
     */
    public static int xpFor(int matchScore, boolean won) {
        return Math.max(0, matchScore) + (won ? WIN_BONUS : 0);
    }

    /**
     * Niveau atteint pour une XP totale cumulée. Démarre à 1.
     */
    public static int levelFor(int xpTotal) {
        if (xpTotal <= 0) {
            return 1;
        }

        return 1 + (int) Math.floor(Math.sqrt(xpTotal / (double) XP_PER_LEVEL_UNIT));
    }

    /**
     * XP totale requise pour atteindre le niveau suivant.
     */
    public static int xpForNextLevel(int level) {
        int current = Math.max(1, level);

        return XP_PER_LEVEL_UNIT * current * current;
    }

    /**
     * Titre honorifique associé à un niveau.
     */
    public static String titleFor(int level) {
        if (level >= 20) {
            return "Maître du savoir";
        }
        if (level >= 10) {
            return "Expert";
        }
        if (level >= 5) {
            return "Confirmé";
        }
        if (level >= 2) {
            return "Apprenti";
        }

        return "Novice";
    }
}
