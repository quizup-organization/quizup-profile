package io.github.quizup.profile.domain.command;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Commandes du profil : une commande par champ modifiable.
 * L'auteur de la modification ({@code requestedBy}) est vérifié dans l'agrégat.
 */
public interface ProfileCommand {

    String userId();

    record CreateProfileCommand(
            @TargetAggregateIdentifier String userId,
            String email,
            String initialPseudonym
    ) implements ProfileCommand {
    }

    record UpdateProfilePseudonymCommand(
            @TargetAggregateIdentifier String userId,
            String requestedBy,
            String pseudonym
    ) implements ProfileCommand {
    }

    record UpdateProfileBioCommand(
            @TargetAggregateIdentifier String userId,
            String requestedBy,
            String bio
    ) implements ProfileCommand {
    }

    record UpdateProfileCountryCommand(
            @TargetAggregateIdentifier String userId,
            String requestedBy,
            String country
    ) implements ProfileCommand {
    }

    record UpdateProfileAvatarCommand(
            @TargetAggregateIdentifier String userId,
            String requestedBy,
            String avatarOptions
    ) implements ProfileCommand {
    }

    record UpdateProfileLanguageCommand(
            @TargetAggregateIdentifier String userId,
            String requestedBy,
            Language language
    ) implements ProfileCommand {
    }
}
