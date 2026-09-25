package io.github.quizup.profile.domain.port.in;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.query.ProfileQuery;

import java.util.concurrent.CompletableFuture;

/**
 * Port entrant - Cas d'utilisation : recherche paginée de profils
 * (par displayName ou email, cf. @Searchable sur la projection).
 */
public interface SearchProfileUseCase {

    CompletableFuture<SearchResponse<Profile>> search(ProfileQuery.ProfileSearchQuery query);

    default CompletableFuture<SearchResponse<Profile>> search(SearchRequest request) {
        return search(new ProfileQuery.ProfileSearchQuery(request));
    }
}
