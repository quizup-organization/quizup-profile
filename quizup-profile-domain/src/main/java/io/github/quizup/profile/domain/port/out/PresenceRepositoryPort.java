package io.github.quizup.profile.domain.port.out;

import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.profile.domain.model.PlayerPresence;

import java.util.Optional;

/**
 * Persistance de la présence joueur (read-model éphémère, hors event sourcing) et des
 * sessions temps réel qui la sous-tendent.
 */
public interface PresenceRepositoryPort {

    PlayerPresence save(PlayerPresence presence);

    Optional<PlayerPresence> findById(String userId);

    /** Recherche paginée (filtres/sorts/pagination standards, ex. {@code userId IN [...]}). */
    PageResult<PlayerPresence> findAll(SearchCriteria searchCriteria);

    /** Enregistre une session temps réel ouverte pour un joueur. */
    void addSession(String sessionId, String userId);

    /** Supprime une session temps réel fermée. */
    void removeSession(String sessionId);

    /** Nombre de sessions temps réel encore ouvertes pour un joueur. */
    long countSessions(String userId);

    /** Purge toutes les sessions (au démarrage : elles meurent avec l'instance). */
    void deleteAllSessions();
}
