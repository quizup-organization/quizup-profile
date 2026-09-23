package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.command.ProfileCommand;

import java.util.concurrent.CompletableFuture;

/**
 * Port entrant - Cas d'utilisation : mise à jour d'un profil (propriétaire uniquement).
 * Implémenté par ProfileCommandService dans application/service/
 */
public interface UpdateProfileUseCase {

    /**
     * Met à jour le profil cible. Le propriétaire est vérifié dans l'agrégat.
     *
     * @param command commande de mise à jour
     * @return un CompletableFuture contenant l'identifiant du profil modifié
     */
    CompletableFuture<String> update(ProfileCommand.UpdateProfileCommand command);

    default CompletableFuture<String> update(
            String userId,
            String requestedBy,
            String displayName,
            String bio,
            String country,
            String avatarOptions) {
        return update(new ProfileCommand.UpdateProfileCommand(
                userId,
                requestedBy,
                displayName,
                bio,
                country,
                avatarOptions
        ));
    }
}
