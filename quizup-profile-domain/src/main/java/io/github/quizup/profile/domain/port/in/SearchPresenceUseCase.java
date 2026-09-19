package io.github.quizup.profile.domain.port.in;

import io.github.quizup.microservice.core.domain.model.search.FilterCriteria;
import io.github.quizup.microservice.core.domain.model.search.PageCriteria;
import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.domain.model.search.SortCriteria;
import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.domain.query.PresenceQuery;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Port entrant — recherche paginée de présences (filtres/sorts/pagination standards, ex.
 * {@code userId IN [...]}).
 */
public interface SearchPresenceUseCase {

    CompletableFuture<PageResult<PlayerPresence>> search(PresenceQuery.PresenceSearchQuery query);

    default CompletableFuture<PageResult<PlayerPresence>> search(List<FilterCriteria> filters,
                                                                 List<SortCriteria> sorts,
                                                                 PageCriteria page) {
        return search(new PresenceQuery.PresenceSearchQuery(filters, sorts, page));
    }
}
