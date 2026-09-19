package io.github.quizup.profile.domain.port.in;

import io.github.quizup.microservice.core.domain.model.search.*;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.query.ProfileQuery;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Port entrant - Cas d'utilisation : recherche paginée de profils
 * (par displayName ou email, cf. @Searchable sur la projection).
 */
public interface SearchProfileUseCase {

    CompletableFuture<PageResult<Profile>> search(ProfileQuery.ProfileSearchQuery query);

    default CompletableFuture<PageResult<Profile>> search(List<FilterCriteria> filters,
                                                          List<SortCriteria> sorts,
                                                          PageCriteria page) {
        return search(
                new ProfileQuery.ProfileSearchQuery(
                        filters,
                        sorts,
                        page
                )
        );
    }
}
