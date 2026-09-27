package io.github.quizup.profile.domain.model;

/**
 * XP attribuée à un joueur pour une partie (journal d'idempotence de la progression).
 */
public record GameXp(String gameId, Integer xp) {
}
