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

    /** Fin explicite ou skip de l'onboarding (idempotent). */
    record CompleteOnboardingCommand(
            @TargetAggregateIdentifier String userId,
            java.time.Instant completedAt
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

    /**
     * Bannière de profil (visuel panoramique affiché derrière l'avatar sur les fiches joueur).
     * {@code bannerUrl} {@code null} efface la bannière.
     */
    record UpdateProfileBannerCommand(
            @TargetAggregateIdentifier String userId,
            String requestedBy,
            String bannerUrl
    ) implements ProfileCommand {
    }

    record UpdateProfileLanguageCommand(
            @TargetAggregateIdentifier String userId,
            String requestedBy,
            Language language
    ) implements ProfileCommand {
    }

    /**
     * Titre thématique **porté** (affiché sur le profil, les cartes et les listes).
     * {@code titleKey} est la clé stable {@code topicId:GRADE} (cf. {@code PlayerTitle}) ;
     * {@code null} efface le titre porté. L'appartenance du titre au joueur (grade réellement
     * atteint dans ce sujet) est vérifiée par l'appelant, qui prépare la commande.
     */
    record UpdateEquippedTitleCommand(
            @TargetAggregateIdentifier String userId,
            String requestedBy,
            String titleKey
    ) implements ProfileCommand {
    }
}
