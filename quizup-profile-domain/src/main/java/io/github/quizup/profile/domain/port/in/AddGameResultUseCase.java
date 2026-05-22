package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.model.GameResult;

import java.util.concurrent.CompletableFuture;

public interface AddGameResultUseCase {

    CompletableFuture<String> addGameResult(ProfileCommand.AddGameResultCommand command);

    default CompletableFuture<String> addGameResult(String profileId, GameResult gameResult) {
        return addGameResult(new ProfileCommand.AddGameResultCommand(profileId, gameResult));
    }
}

