package io.github.quizup.profile.application.handler.query;

import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.domain.port.out.PresenceRepositoryPort;
import io.github.quizup.profile.domain.query.PresenceQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

/**
 * Handler Axon — recherche paginée de présences. Délègue au port sortant.
 */
@Component
public class PresenceQueryHandler {

    private final PresenceRepositoryPort presenceRepositoryPort;

    public PresenceQueryHandler(PresenceRepositoryPort presenceRepositoryPort) {
        this.presenceRepositoryPort = presenceRepositoryPort;
    }

    @QueryHandler
    public PageResult<PlayerPresence> handle(PresenceQuery.PresenceSearchQuery query) {
        return presenceRepositoryPort.findAll(query);
    }
}
