package io.github.quizup.profile.domain.port.out;

import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.profile.domain.model.PlayerPresence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistance de la présence joueur (read-model éphémère, hors event sourcing) et des
 * sessions temps réel qui la sous-tendent.
 */
public interface PresenceRepositoryPort {

    PlayerPresence save(PlayerPresence presence);

    Optional<PlayerPresence> findById(String userId);

    /** Recherche paginée (filtres/sorts/pagination standards, ex. {@code userId IN [...]}). */
    SearchResponse<PlayerPresence> findAll(SearchRequest request);

    /** Résolution en lot (les joueurs sans ligne sont absents du résultat). */
    List<PlayerPresence> findByIds(List<String> userIds);

    /** Enregistre une session temps réel ouverte pour un joueur (rattachée à une instance BFF). */
    void addSession(String sessionId, String userId, String instanceId);

    /** Supprime une session temps réel fermée. */
    void removeSession(String sessionId);

    /** Nombre de sessions temps réel encore ouvertes pour un joueur. */
    long countSessions(String userId);

    /** Renouvelle le bail ({@code last_seen_at}) des sessions fournies (heartbeat batch du BFF). */
    void touchSessions(List<String> sessionIds, Instant now);

    /** Joueurs ayant au moins une session sans renouvellement depuis {@code before}. */
    List<String> findStaleSessionUserIds(Instant before);

    /** Supprime les sessions sans renouvellement depuis {@code before} (instance BFF disparue). */
    void deleteStaleSessions(Instant before);

    /** Joueurs ayant au moins une session ouverte sur l'instance BFF donnée, ouverte avant {@code before}. */
    List<String> userIdsByInstanceBefore(String instanceId, Instant before);

    /** Supprime les sessions d'une instance BFF ouvertes avant {@code before} (purge au redémarrage). */
    void deleteSessionsByInstanceBefore(String instanceId, Instant before);

    /** Présences en ligne dont l'échéance hors ligne est dépassée (aucune session restante). */
    List<PlayerPresence> findDueOffline(Instant now);

    /**
     * Transition conditionnelle {@code ONLINE → OFFLINE} si l'échéance est toujours dépassée et
     * qu'aucune session n'existe (idempotent, sûr entre instances).
     *
     * @return {@code true} si la ligne a effectivement basculé (à publier), {@code false} sinon.
     */
    boolean markOffline(String userId, Instant now);
}
