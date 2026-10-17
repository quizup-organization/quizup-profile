package io.github.quizup.profile.domain.model;

/**
 * Famille d'un {@link Badge} : ce que le badge célèbre. Sert au regroupement dans la collection
 * (et à l'affichage par sections). **Une famille = une échelle** (12 échelles × 7 paliers + les
 * 4 exploits ponctuels) : aucune famille ne mélange deux compteurs.
 */
public enum BadgeFamily {

    /** Volume : **duels** joués entre joueurs. */
    VOLUME,

    /** Victoires : **duels gagnés** entre joueurs. */
    VICTORY,

    /** Maîtrise d'un sujet (grade atteint). */
    MASTERY,

    /** Exploit ponctuel d'un duel (sans faute, éclair, série…). */
    EXPLOIT,

    /** Série : victoires consécutives. */
    SERIES,

    /** Assiduité : jours joués consécutifs. */
    ASSIDUITY,

    /** Social : abonnés gagnés. */
    SOCIAL,

    /** Défis : défis nominatifs lancés. */
    CHALLENGES,

    /** Vision : bonnes réponses sur les questions illustrées. */
    VISION,

    /** Précision : duels parfaits cumulés. */
    PRECISION,

    /** Rapidité : réponses rapides cumulées. */
    SPEED,

    /** Exploration : nombre de sujets différents joués. */
    EXPLORATION,

    /** Collection : nombre de badges obtenus. */
    COLLECTION
}
