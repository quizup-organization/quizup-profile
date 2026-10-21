package io.github.quizup.profile.domain.model;

/**
 * Règles Elo globales du profil (pures, sans dépendance framework).
 *
 * <p>L'Elo mesure la compétence en duel, tous sujets confondus. L'XP de sujet reste
 * indépendante : elle porte la maîtrise, les titres et les classements par thème.</p>
 *
 * <p>Départ à {@value #INITIAL_RATING} (échelle type chess.com), provisoire sur les
 * {@value #PROVISIONAL_GAMES} premiers duels classés, puis K réduit. Les duels contre bot ne
 * sont jamais classés.</p>
 */
public final class EloRules {

    public static final int INITIAL_RATING = 200;
    public static final int PROVISIONAL_GAMES = 10;
    public static final int K_PROVISIONAL = 40;
    public static final int K_RATED = 20;

    /** Nombre maximum de duels comptés pour l'Elo contre le même adversaire, par jour. */
    public static final int DAILY_OPPONENT_CAP = 2;

    private EloRules() {
    }

    /** Score attendu du joueur (0–1) selon l'écart de rating. */
    public static double expectedScore(int rating, int opponentRating) {
        return 1.0 / (1.0 + Math.pow(10.0, (opponentRating - rating) / 400.0));
    }

    /** Facteur K : plus élevé pendant la période provisoire. */
    public static int kFactor(int ratedGames) {
        return ratedGames < PROVISIONAL_GAMES ? K_PROVISIONAL : K_RATED;
    }

    /**
     * Nouveau rating après un duel.
     *
     * @param rating         rating courant du joueur
     * @param opponentRating rating courant de l'adversaire
     * @param score          1 victoire, 0.5 nul, 0 défaite
     * @param ratedGames     nombre de duels classés déjà joués (avant ce duel)
     * @return nouveau rating, borné à 0
     */
    public static int newRating(int rating, int opponentRating, double score, int ratedGames) {
        double expected = expectedScore(rating, opponentRating);
        int next = (int) Math.round(rating + kFactor(ratedGames) * (score - expected));
        return Math.max(0, next);
    }

    public static boolean isProvisional(int ratedGames) {
        return ratedGames < PROVISIONAL_GAMES;
    }
}
