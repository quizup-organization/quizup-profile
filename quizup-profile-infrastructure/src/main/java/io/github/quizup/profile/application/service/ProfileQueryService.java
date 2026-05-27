package io.github.quizup.profile.application.service;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.infrastructure.axon.PageResponseTypes;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.model.ProfileTopic;
import io.github.quizup.profile.domain.port.in.GetProfileUseCase;
import io.github.quizup.profile.domain.port.in.SearchProfileUseCase;
import io.github.quizup.profile.domain.port.in.SearchProfileTopicsUseCase;
import io.github.quizup.profile.domain.query.ProfileQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ProfileQueryService implements GetProfileUseCase, SearchProfileUseCase, SearchProfileTopicsUseCase {

    private final QueryGateway queryGateway;

    public ProfileQueryService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public CompletableFuture<Profile> getById(ProfileQuery.GetProfileByIdQuery query) {
        return queryGateway.query(query, ResponseTypes.instanceOf(Profile.class));
    }

    @Override
    public CompletableFuture<PageResult<Profile>> search(ProfileQuery.SearchProfileQuery query) {
        return queryGateway.query(query, PageResponseTypes.pageResultOf(Profile.class));
    }

    @Override
    public CompletableFuture<PageResult<ProfileTopic>> searchTopics(ProfileQuery.SearchProfileTopicsQuery query) {
        return queryGateway.query(query, PageResponseTypes.pageResultOf(ProfileTopic.class));
    }
}

