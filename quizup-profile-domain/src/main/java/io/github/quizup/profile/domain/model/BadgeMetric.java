package io.github.quizup.profile.domain.model;

/**
 * Compteur sur lequel se mesure un {@link Badge} — permet d'afficher une **progression**
 * (« 7/10 duels ») et non seulement un état acquis/verrouillé.
 *
 * <p>Toutes les métriques sont des <b>faits de duels entre joueurs</b> : un entraînement contre
 * bot ne fait progresser aucun badge (même règle que l'XP).</p>
 */
public enum BadgeMetric {

    /** Duels **entre joueurs** joués ({@code gamesPlayed}). */
    HUMAN_DUELS,

    /** Duels entre joueurs **gagnés** ({@code wins}). */
    HUMAN_WINS,

    /** XP cumulée du sujet le mieux maîtrisé ({@code max(xpByTopic)}). */
    TOPIC_MASTERY,

    /** Nombre de sujets joués (XP > 0) — l'entraînement (0 XP) ne compte pas. */
    DISTINCT_TOPICS,

    /** Victoires consécutives, tous sujets confondus ({@code winStreak} au moment du duel). */
    WIN_STREAK,

    /** Duels parfaits cumulés (score maximal atteint). */
    PERFECT_DUELS,

    /** Réponses rapides cumulées (tous duels confondus). */
    FAST_ANSWERS,

    /** Meilleure série de jours joués consécutifs (assiduité). */
    DAILY_STREAK,

    /** Abonnés gagnés (cumulés, se désabonner ne retire rien) — famille Social. */
    FOLLOWERS_GAINED,

    /** Défis nominatifs lancés — famille Défis. */
    CHALLENGES_SENT,

    /** Bonnes réponses cumulées sur les questions illustrées — famille Vision. */
    IMAGE_CORRECT,

    /** Nombre de badges obtenus (famille {@code COLLECTION}). */
    EARNED_BADGES,

    /** Fait ponctuel : acquis d'un coup, aucune progression intermédiaire. */
    ONE_SHOT
}
