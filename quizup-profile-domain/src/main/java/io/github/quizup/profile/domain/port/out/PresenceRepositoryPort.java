package io.github.quizup.profile.domain.port.out;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.profile.domain.model.PlayerPresence;

import java.util.List;
import java.util.Optional;

/**
 * Projection durable de la présence joueur (transitions {@code ONLINE}/{@code OFFLINE}).
 * La vérité temps réel vit dans le store chaud ({@link PresenceLeasePort}) ; cette projection
 * alimente les lectures produit et l'historique {@code lastSeenAt}.
 */
public interface PresenceRepositoryPort {

    PlayerPresence save(PlayerPresence presence);

    Optional<PlayerPresence> findById(String userId);

    /** Recherche paginée (filtres/sorts/pagination standards, ex. {@code userId IN [...]}). */
    SearchResponse<PlayerPresence> findAll(SearchRequest request);

    /** Résolution en lot (les joueurs sans ligne sont absents du résultat). */
    List<PlayerPresence> findByIds(List<String> userIds);
}
