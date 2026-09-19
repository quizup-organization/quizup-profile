package io.github.quizup.profile.application.service;

import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.TopicProgress;
import io.github.quizup.profile.domain.port.in.GetProgressionUseCase;
import io.github.quizup.profile.domain.query.ProgressionQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service applicatif — implémente {@link GetProgressionUseCase} via le
 * {@link QueryGateway} (même pattern que ProfileQueryService).
 */
@Service
public class ProgressionQueryService implements GetProgressionUseCase {

    private final QueryGateway queryGateway;

    public ProgressionQueryService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public CompletableFuture<PlayerProgress> getById(ProgressionQuery.GetProgressionQuery query) {
        return queryGateway.query(query, QueryResponseTypes.instanceOf(PlayerProgress.class));
    }

    @Override
    public CompletableFuture<TopicProgress> getTopic(ProgressionQuery.GetTopicProgressionQuery query) {
        return queryGateway.query(query, QueryResponseTypes.instanceOf(TopicProgress.class));
    }
}
