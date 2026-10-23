package io.github.quizup.profile.domain.model;

/**
 * Récompense persistée débloquée par un joueur. Les récompenses sont distinctes des badges :
 * un badge est une réussite, une récompense est un droit débloqué (ex. style d'avatar).
 */
public enum RewardCode {
    /** Style d'avatar Micah, débloqué par le badge d'assiduité Bronze (7 jours). */
    MICAH_AVATAR
}
