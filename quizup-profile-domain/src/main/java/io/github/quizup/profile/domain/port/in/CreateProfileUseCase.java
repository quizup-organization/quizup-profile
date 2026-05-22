package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.command.ProfileCommand;

import java.util.concurrent.CompletableFuture;

public interface CreateProfileUseCase {

    CompletableFuture<String> create(ProfileCommand.CreateProfileCommand command);

    default CompletableFuture<String> create(String profileId) {
        return create(new ProfileCommand.CreateProfileCommand(profileId));
    }
}

