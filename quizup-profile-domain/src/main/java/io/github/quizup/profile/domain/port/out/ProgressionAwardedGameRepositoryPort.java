package io.github.quizup.profile.domain.port.out;

import io.github.quizup.profile.domain.model.GameXp;

import java.util.List;

/**
 * Port sortant - journal des attributions d'XP déjà appliquées (clé {@code userId + gameId}).
 *
 * <p>Rend la projection de progression idempotente : un {@code XpAwardedEvent} rejoué
 * n'est pas recompté. Porte aussi l'XP réellement attribuée par partie.</p>
 */
public interface ProgressionAwardedGameRepositoryPort {

    /**
     * Enregistre l'attribution d'XP d'une partie pour un joueur.
     *
     * @return {@code true} si c'est une nouvelle attribution (à appliquer), {@code false} sinon.
     */
    boolean record(String userId, String gameId, int xp);

    /**
     * XP attribuée pour une liste de parties (les parties inconnues sont absentes du résultat).
     */
    List<GameXp> findGameXp(String userId, List<String> gameIds);
}
