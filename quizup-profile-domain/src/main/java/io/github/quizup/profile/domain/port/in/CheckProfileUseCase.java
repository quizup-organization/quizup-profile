package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.query.ProfileQuery;

import java.util.concurrent.CompletableFuture;

public interface CheckProfileUseCase {

    CompletableFuture<Boolean> existsById(ProfileQuery.ProfileExistsByIdQuery query);

    default CompletableFuture<Boolean> existsById(String profileId) {
        return existsById(new ProfileQuery.ProfileExistsByIdQuery(profileId));
    }
}

