package io.github.quizup.profile.application.service;

import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.profile.domain.exception.ProfileProblems;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.port.in.CheckProfileUseCase;
import io.github.quizup.profile.domain.port.in.GetProfileUseCase;
import io.github.quizup.profile.domain.port.in.SearchProfileUseCase;
import io.github.quizup.profile.domain.query.ProfileQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service applicatif - Implémente GetProfileUseCase, SearchProfileUseCase et CheckProfileUseCase.
 * Cette classe est autorisée à connaitre QueryGateway (couche application).
 */
@Service
public class ProfileQueryService implements GetProfileUseCase, SearchProfileUseCase, CheckProfileUseCase {

    private final QueryGateway queryGateway;

    public ProfileQueryService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public CompletableFuture<Profile> getById(ProfileQuery.GetProfileQuery query) throws ProfileProblems.ProfileNotFoundProblem {
        return queryGateway.query(query, QueryResponseTypes.instanceOf(Profile.class));
    }

    @Override
    public CompletableFuture<PageResult<Profile>> search(ProfileQuery.ProfileSearchQuery query) {
        return queryGateway.query(query, QueryResponseTypes.pageResultOf(Profile.class));
    }

    @Override
    public CompletableFuture<Boolean> existsById(ProfileQuery.ProfileExistsByIdQuery query) {
        return queryGateway.query(query, QueryResponseTypes.instanceOf(Boolean.class));
    }
}
