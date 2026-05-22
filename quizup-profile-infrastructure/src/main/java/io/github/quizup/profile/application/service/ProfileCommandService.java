package io.github.quizup.profile.application.service;

import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.port.in.AddGameResultUseCase;
import io.github.quizup.profile.domain.port.in.CreateProfileUseCase;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ProfileCommandService implements CreateProfileUseCase, AddGameResultUseCase {

    private final CommandGateway commandGateway;

    public ProfileCommandService(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @Override
    public CompletableFuture<String> create(ProfileCommand.CreateProfileCommand command) {
        return commandGateway.send(command);
    }

    @Override
    public CompletableFuture<String> addGameResult(ProfileCommand.AddGameResultCommand command) {
        return commandGateway.send(command);
    }
}

