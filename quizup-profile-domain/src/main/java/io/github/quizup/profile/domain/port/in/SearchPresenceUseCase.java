package io.github.quizup.profile.domain.port.in;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.domain.query.PresenceQuery;

import java.util.concurrent.CompletableFuture;

/**
 * Port entrant — recherche paginée de présences (filtres/sorts/pagination standards, ex.
 * {@code userId IN [...]}).
 */
public interface SearchPresenceUseCase {

    CompletableFuture<SearchResponse<PlayerPresence>> search(PresenceQuery.PresenceSearchQuery query);

    default CompletableFuture<SearchResponse<PlayerPresence>> search(SearchRequest request) {
        return search(new PresenceQuery.PresenceSearchQuery(request));
    }
}
