package io.github.quizup.profile.domain.model;

/**
 * XP et variation Elo attribuées à un joueur pour une partie (journal d'idempotence de la
 * progression). {@code rated=false} pour un entraînement bot ou un duel hors plafond anti-farm.
 */
public record GameXp(String gameId, Integer xp, int ratingDelta, boolean rated) {
}
