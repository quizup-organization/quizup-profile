package io.github.quizup.profile.application.handler.query;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.profile.domain.exception.ProfileProblems;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.model.ProfileTopic;
import io.github.quizup.profile.domain.port.out.ProfileRepositoryPort;
import io.github.quizup.profile.domain.query.ProfileQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class ProfileQueryHandler {

    private final ProfileRepositoryPort profileRepositoryPort;

    public ProfileQueryHandler(ProfileRepositoryPort profileRepositoryPort) {
        this.profileRepositoryPort = profileRepositoryPort;
    }

    @QueryHandler
    public Profile handle(ProfileQuery.GetProfileByIdQuery query) {
        return profileRepositoryPort.findById(query.profileId())
                .orElseThrow(() -> new ProfileProblems.ProfileNotFoundProblem(query.profileId()));
    }

    @QueryHandler
    public boolean handle(ProfileQuery.ProfileExistsByIdQuery query) {
        return profileRepositoryPort.existsById(query.profileId());
    }

    @QueryHandler
    public PageResult<Profile> handle(ProfileQuery.SearchProfileQuery query) {
        return profileRepositoryPort.findAll(query);
    }

    @QueryHandler
    public PageResult<ProfileTopic> handle(ProfileQuery.SearchProfileTopicsQuery query) {
        if (!profileRepositoryPort.existsById(query.profileId())) {
            throw new ProfileProblems.ProfileNotFoundProblem(query.profileId());
        }
        return profileRepositoryPort.findTopicsByProfileId(query.profileId(), query);
    }
}

