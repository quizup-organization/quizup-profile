package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.exception.ProfileProblems;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.query.ProfileQuery;

import java.util.concurrent.CompletableFuture;

public interface GetProfileUseCase {

    CompletableFuture<Profile> getById(ProfileQuery.GetProfileByIdQuery query) throws ProfileProblems.ProfileNotFoundProblem;

    default CompletableFuture<Profile> getById(String profileId) throws ProfileProblems.ProfileNotFoundProblem {
        return getById(new ProfileQuery.GetProfileByIdQuery(profileId));
    }
}

