package io.github.quizup.profile.application.service;

import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.port.in.CreateProfileUseCase;
import io.github.quizup.profile.domain.port.in.UpdateProfileUseCase;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service applicatif - Implémente CreateProfileUseCase et UpdateProfileUseCase (une méthode par champ).
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
    public CompletableFuture<String> updatePseudonym(ProfileCommand.UpdateProfilePseudonymCommand command) {
        return commandGateway.send(command);
    }

    @Override
    public CompletableFuture<String> updateBio(ProfileCommand.UpdateProfileBioCommand command) {
        return commandGateway.send(command);
    }

    @Override
    public CompletableFuture<String> updateCountry(ProfileCommand.UpdateProfileCountryCommand command) {
        return commandGateway.send(command);
    }

    @Override
    public CompletableFuture<String> updateAvatar(ProfileCommand.UpdateProfileAvatarCommand command) {
        return commandGateway.send(command);
    }

    @Override
    public CompletableFuture<String> updateLanguage(ProfileCommand.UpdateProfileLanguageCommand command) {
        return commandGateway.send(command);
    }
}
