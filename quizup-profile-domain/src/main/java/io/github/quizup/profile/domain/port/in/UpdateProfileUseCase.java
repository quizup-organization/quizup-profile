package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.command.ProfileCommand;

import java.util.concurrent.CompletableFuture;

/**
 * Port entrant - Cas d'utilisation : mise à jour d'un profil (propriétaire uniquement).
 * Une méthode par champ ; l'agrégat vérifie la propriété et n'émet un événement que si la
 * valeur change.
 */
public interface UpdateProfileUseCase {

    CompletableFuture<String> updatePseudonym(ProfileCommand.UpdateProfilePseudonymCommand command);

    CompletableFuture<String> updateBio(ProfileCommand.UpdateProfileBioCommand command);

    CompletableFuture<String> updateCountry(ProfileCommand.UpdateProfileCountryCommand command);

    CompletableFuture<String> updateAvatar(ProfileCommand.UpdateProfileAvatarCommand command);

    CompletableFuture<String> updateLanguage(ProfileCommand.UpdateProfileLanguageCommand command);
}
