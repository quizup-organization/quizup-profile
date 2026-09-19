package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.command.ProfileCommand;

import java.util.concurrent.CompletableFuture;

/**
 * Port entrant — création d'un profil. Utilisé par le seeding système ; en fonctionnement
 * nominal la création est déclenchée par {@code CreateProfileSaga} sur {@code UserRegisteredEvent}.
 */
public interface CreateProfileUseCase {

    CompletableFuture<String> create(ProfileCommand.CreateProfileCommand command);

    default CompletableFuture<String> create(String userId, String email, String displayName) {
        return create(new ProfileCommand.CreateProfileCommand(userId, email, displayName));
    }
}
