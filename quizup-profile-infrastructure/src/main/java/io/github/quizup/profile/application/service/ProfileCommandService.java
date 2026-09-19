package io.github.quizup.profile.application.service;

import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.port.in.CreateProfileUseCase;
import io.github.quizup.profile.domain.port.in.UpdateProfileUseCase;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service applicatif - Implémente UpdateProfileUseCase et CreateProfileUseCase.
 */
@Service
public class ProfileCommandService implements UpdateProfileUseCase, CreateProfileUseCase {

    private final CommandGateway commandGateway;

    public ProfileCommandService(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @Override
    public CompletableFuture<String> create(ProfileCommand.CreateProfileCommand command) {
        return commandGateway.send(command);
    }

    @Override
    public CompletableFuture<String> update(ProfileCommand.UpdateProfileCommand command) {
        return commandGateway.send(command);
    }
}
