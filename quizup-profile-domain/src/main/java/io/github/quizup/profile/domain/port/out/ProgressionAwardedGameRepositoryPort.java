package io.github.quizup.profile.domain.port.out;

/**
 * Port sortant - journal des attributions d'XP déjà appliquées (clé {@code userId + gameId}).
 *
 * <p>Rend la projection de progression idempotente : un {@code XpAwardedEvent} rejoué
 * n'est pas recompté.</p>
 */
public interface ProgressionAwardedGameRepositoryPort {

    /**
     * Enregistre l'attribution d'XP d'une partie pour un joueur.
     *
     * @return {@code true} si c'est une nouvelle attribution (à appliquer), {@code false} sinon.
     */
    boolean record(String userId, String gameId);
}
