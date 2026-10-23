package io.github.quizup.profile.domain.model;

/**
 * Règles de progression **purement thématiques** : XP gagnée et identifiants namespacés des
 * agrégats.
 *
 * <p><b>XP</b> : {@code score du match + bonus de victoire} — un duel contre bot est un
 * <b>entraînement</b> et n'attribue aucune XP ({@link #xpFor(int, boolean, boolean)}). L'XP est
 * une métrique <b>par sujet</b> ; elle alimente les grades de maîtrise et les titres thématiques.
 * Il n'existe plus d'XP globale ni de niveau de compte : l'Elo est la seule métrique au niveau
 * du joueur.</p>
 *
 * <p><b>Maîtrise par sujet</b> : échelle en {@link MasteryGrade grades}, sur l'XP cumulée
 * <i>dans le sujet</i> (cf. {@link MasteryGrade#gradeFor(int)}).</p>
 */
public final class ProgressionRules {

    /** Score maximal d'un duel (6 questions normales + 1 bonus) — cf. spec produit. */
    public static final int PERFECT_SCORE = 160;

    /** Bonus d'XP accordé pour une victoire. */
    public static final int WIN_BONUS = 50;

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
     * Identifiant de l'agrégat **badges** d'un joueur ({@code badges:<userId>}) — même convention
     * de namespace que {@link #progressIdFor(String)} (les agrégats ne partagent jamais un id).
     */
    public static String badgeIdFor(String userId) {
        return "badges:" + userId;
    }

    /**
     * XP gagnée à l'issue d'un duel.
     *
     * <p>Un duel contre bot est un <b>entraînement</b> : il n'attribue aucune XP — la
     * progression ne vient que des duels entre joueurs. Le duel reste projeté (badges, série de
     * jours joués, historique) mais son XP vaut 0, quel que soit le score.</p>
     *
     * @param matchScore score obtenu dans le duel
     * @param won        vrai si le joueur a gagné
     * @param botGame    vrai pour un entraînement contre bot (aucune XP)
     * @return XP (jamais négative)
     */
    public static int xpFor(int matchScore, boolean won, boolean botGame) {
        if (botGame) {
            return 0;
        }

        return Math.max(0, matchScore) + (won ? WIN_BONUS : 0);
    }
}
