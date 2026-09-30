package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.exception.ProfileProblems;
import io.github.quizup.profile.domain.model.ProfileRules;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.Instant;
import java.util.Objects;

/**
 * ProfileAggregate - Gère le cycle de vie du profil modifiable d'un utilisateur.
 * Créé par la saga suite à un UserRegisteredEvent du service identity,
 * mis à jour uniquement par le propriétaire du profil, champ par champ.
 */
@Aggregate
public class ProfileAggregate {

    @AggregateIdentifier
    private String userId;

    private String email;
    private String pseudonym;
    private String bio;
    private String country;
    private String avatarOptions;
    private Language language;

    // Constructeur par défaut requis par Axon
    protected ProfileAggregate() {
    }

    @CommandHandler
    public ProfileAggregate(ProfileCommand.CreateProfileCommand command) {
        validatePseudonym(command.userId(), command.initialPseudonym());

        AggregateLifecycle.apply(
                new ProfileEvent.ProfileCreatedEvent(
                        command.userId(),
                        command.email(),
                        command.initialPseudonym(),
                        Instant.now()
                )
        );
    }

    @CommandHandler
    public void handle(ProfileCommand.UpdateProfilePseudonymCommand command) {
        requireOwner(command.requestedBy());
        validatePseudonym(command.userId(), command.pseudonym());
        if (Objects.equals(command.pseudonym(), this.pseudonym)) {
            return;
        }

        AggregateLifecycle.apply(
                new ProfileEvent.ProfilePseudonymUpdatedEvent(
                        command.userId(),
                        command.requestedBy(),
                        command.pseudonym(),
                        Instant.now()
                )
        );
    }

    @CommandHandler
    public void handle(ProfileCommand.UpdateProfileBioCommand command) {
        requireOwner(command.requestedBy());
        validateBio(command.userId(), command.bio());
        if (Objects.equals(command.bio(), this.bio)) {
            return;
        }

        AggregateLifecycle.apply(
                new ProfileEvent.ProfileBioUpdatedEvent(
                        command.userId(),
                        command.requestedBy(),
                        command.bio(),
                        Instant.now()
                )
        );
    }

    @CommandHandler
    public void handle(ProfileCommand.UpdateProfileCountryCommand command) {
        requireOwner(command.requestedBy());
        validateCountry(command.userId(), command.country());
        if (Objects.equals(command.country(), this.country)) {
            return;
        }

        AggregateLifecycle.apply(
                new ProfileEvent.ProfileCountryUpdatedEvent(
                        command.userId(),
                        command.requestedBy(),
                        command.country(),
                        Instant.now()
                )
        );
    }

    @CommandHandler
    public void handle(ProfileCommand.UpdateProfileAvatarCommand command) {
        requireOwner(command.requestedBy());
        validateAvatarOptions(command.userId(), command.avatarOptions());
        if (Objects.equals(command.avatarOptions(), this.avatarOptions)) {
            return;
        }

        AggregateLifecycle.apply(
                new ProfileEvent.ProfileAvatarUpdatedEvent(
                        command.userId(),
                        command.requestedBy(),
                        command.avatarOptions(),
                        Instant.now()
                )
        );
    }

    @CommandHandler
    public void handle(ProfileCommand.UpdateProfileLanguageCommand command) {
        requireOwner(command.requestedBy());
        if (command.language() == null) {
            throw new ProfileProblems.LanguageMissingProblem(command.userId());
        }
        if (command.language() == this.language) {
            return;
        }

        AggregateLifecycle.apply(
                new ProfileEvent.ProfileLanguageUpdatedEvent(
                        command.userId(),
                        command.requestedBy(),
                        command.language(),
                        Instant.now()
                )
        );
    }

    @EventSourcingHandler
    public void on(ProfileEvent.ProfileCreatedEvent event) {
        this.userId = event.userId();
        this.email = event.email();
        this.pseudonym = event.pseudonym();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.ProfilePseudonymUpdatedEvent event) {
        this.pseudonym = event.pseudonym();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.ProfileBioUpdatedEvent event) {
        this.bio = event.bio();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.ProfileCountryUpdatedEvent event) {
        this.country = event.country();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.ProfileAvatarUpdatedEvent event) {
        this.avatarOptions = event.avatarOptions();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.ProfileLanguageUpdatedEvent event) {
        this.language = event.language();
    }

    private void requireOwner(String requestedBy) {
        if (!userId.equals(requestedBy)) {
            throw new ProfileProblems.ProfileNotOwnerProblem(userId, requestedBy);
        }
    }

    private static void validatePseudonym(String userId, String pseudonym) {
        if (pseudonym == null || pseudonym.trim().isEmpty()) {
            throw new ProfileProblems.PseudonymBlankProblem(userId, pseudonym);
        }
        if (pseudonym.length() > ProfileRules.MAX_PSEUDONYM_LENGTH) {
            throw new ProfileProblems.PseudonymTooLongProblem(userId, pseudonym);
        }
    }

    private static void validateBio(String userId, String bio) {
        if (bio != null && bio.length() > ProfileRules.MAX_BIO_LENGTH) {
            throw new ProfileProblems.BioTooLongProblem(userId, bio);
        }
    }

    private static void validateCountry(String userId, String country) {
        if (country != null && country.length() > ProfileRules.MAX_COUNTRY_LENGTH) {
            throw new ProfileProblems.CountryTooLongProblem(userId, country);
        }
    }

    private static void validateAvatarOptions(String userId, String avatarOptions) {
        if (avatarOptions != null && avatarOptions.length() > ProfileRules.MAX_AVATAR_OPTIONS_LENGTH) {
            throw new ProfileProblems.AvatarOptionsTooLongProblem(userId);
        }
    }
}
