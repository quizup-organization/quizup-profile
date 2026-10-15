package io.github.quizup.profile.domain.model;

/**
 * Famille d'un {@link Badge} : ce que le badge célèbre. Sert au regroupement dans la collection
 * (et à l'affichage par sections « Volume / Maîtrise / Exploit / Collection »).
 */
public enum BadgeFamily {

    /** Volume de jeu entre joueurs (duels joués, victoires). */
    VOLUME,

    /** Maîtrise d'un sujet (grade atteint). */
    MASTERY,

    /** Exploit ponctuel d'un duel (sans faute, éclair, série…). */
    EXPLOIT,

    /** Série : victoires consécutives. */
    SERIES,

    /** Exploration : nombre de sujets différents joués. */
    EXPLORATION,

    /** Collection : nombre de badges obtenus. */
    COLLECTION
}
