package io.github.quizup.profile.domain.model;

/**
 * Règles de progression (pures, sans dépendance framework) : XP gagnée, niveau de compte et
 * **grade de maîtrise d'un sujet**.
 *
 * <p><b>XP</b> : {@code score du match + bonus de victoire} — un duel contre bot est un
 * <b>entraînement</b> et n'attribue aucune XP ({@link #xpFor(int, boolean, boolean)}).</p>
 *
 * <p><b>Courbe de niveaux</b> (compte) : explicite et **à départ accéléré**. Le palier qui mène
 * du niveau {@code L} au niveau {@code L+1} coûte {@code 100·L − 20} XP de plus que le
 * précédent, soit une XP cumulée de {@code seuil(L) = (L−1)·(50L − 20)} pour atteindre le
 * niveau {@code L}. Jalon (à ~120 XP par duel) :</p>
 *
 * <pre>
 * N2  =    80 XP (≈1 duel)      N10 =  4 320 XP (≈36 duels)
 * N5  =   920 XP (≈8 duels)     N20 = 18 620 XP (≈155 duels)
 * </pre>
 *
 * <p>Les premiers paliers tombent vite — la rétention se joue là — puis la courbe se ralentit
 * (croissance quadratique).</p>
 *
 * <p><b>Maîtrise par sujet</b> : échelle distincte, en {@link MasteryGrade grades}, sur l'XP
 * cumulée <i>dans le sujet</i> (cf. {@link MasteryGrade#gradeFor(int)}) — c'est elle qui porte
 * les titres thématiques.</p>
 */
public final class ProgressionRules {

    /** Score maximal d'un duel (6 questions normales + 1 bonus) — cf. spec produit. */
    public static final int PERFECT_SCORE = 160;

    /** Bonus d'XP accordé pour une victoire. */
    public static final int WIN_BONUS = 50;

    /**
     * Facteur du seuil de niveau : {@code seuil(L) = (L−1)·(LEVEL_STEP_FACTOR·L − LEVEL_STEP_OFFSET)},
     * soit un palier de {@code 100·L − 20} XP entre les niveaux {@code L} et {@code L+1}.
     */
    private static final int LEVEL_STEP_FACTOR = 50;

    private static final int LEVEL_STEP_OFFSET = 20;

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

    /**
     * XP totale cumulée nécessaire pour atteindre un niveau ({@code 0} pour le niveau 1).
     */
    public static int xpThreshold(int level) {
        int current = Math.max(1, level);

        return (current - 1) * (LEVEL_STEP_FACTOR * current - LEVEL_STEP_OFFSET);
    }

    /**
     * Niveau de compte atteint pour une XP totale cumulée. Démarre à 1.
     */
    public static int levelFor(int xpTotal) {
        if (xpTotal <= 0) {
            return 1;
        }

        int level = 1;
        while (xpThreshold(level + 1) <= xpTotal) {
            level++;
        }

        return level;
    }

    /**
     * XP totale cumulée à atteindre pour passer au niveau suivant (contrat inchangé : c'est bien
     * une XP <b>totale</b>, pas un palier relatif).
     */
    public static int xpForNextLevel(int level) {
        return xpThreshold(Math.max(1, level) + 1);
    }

    /**
     * Titre de <b>compte</b> associé à un niveau.
     *
     * <p>Transitoire : les titres sont désormais **thématiques** — un titre est un
     * {@link MasteryGrade} de maîtrise d'un sujet ({@link PlayerTitle}), jamais un titre de
     * compte. Cette échelle générique reste le temps de la bascule de l'affichage (elle n'est
     * plus qu'un libellé, sans effet sur la progression).</p>
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
