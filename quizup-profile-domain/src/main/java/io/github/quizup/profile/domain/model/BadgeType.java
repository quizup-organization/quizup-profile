package io.github.quizup.profile.domain.model;

/**
 * Badges déblocables par un joueur.
 * Vérifier dans ProfileAggregate.computeNewBadges() les conditions d'attribution.
 */
public enum BadgeType {

    /** Première victoire. */
    FIRST_WIN,

    /** Score parfait (160/160) sur un duel. */
    PERFECT_SCORE,

    /** 5 victoires consécutives dans un même thème. */
    FIRE_STREAK_5,

    /** 10 victoires consécutives dans un même thème. */
    FIRE_STREAK_10,

    /** 100 parties jouées au total. */
    VETERAN_100,

    /** Atteindre le niveau 10 sur un thème. */
    SPECIALIST
}