package io.github.quizup.profile.application.service;

import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.domain.port.in.SearchPresenceUseCase;
import io.github.quizup.profile.domain.query.PresenceQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service applicatif — recherche paginée de présences via le bus de queries Axon.
 */
@Service
public class PresenceQueryService implements SearchPresenceUseCase {

    private final QueryGateway queryGateway;

    public PresenceQueryService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public CompletableFuture<PageResult<PlayerPresence>> search(PresenceQuery.PresenceSearchQuery query) {
        return queryGateway.query(query, QueryResponseTypes.pageResultOf(PlayerPresence.class));
    }
}
