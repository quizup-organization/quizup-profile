package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.command.ProfileCommand;

import java.util.concurrent.CompletableFuture;

public interface AddGameResultUseCase {

    CompletableFuture<String> addGameResult(ProfileCommand.AddGameResultCommand command);
}

