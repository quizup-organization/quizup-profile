package io.github.quizup.profile.domain.aggregate;

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

/**
 * ProfileAggregate - Gère le cycle de vie du profil modifiable d'un utilisateur.
 * Créé par la saga suite à un UserRegisteredEvent du service identity,
 * mis à jour uniquement par le propriétaire du profil.
 */
@Aggregate
public class ProfileAggregate {

    @AggregateIdentifier
    private String userId;

    private String email;
    private String displayName;
    private String bio;
    private String country;

    // Constructeur par défaut requis par Axon
    protected ProfileAggregate() {
    }

    @CommandHandler
    public ProfileAggregate(ProfileCommand.CreateProfileCommand command) {
        validateDisplayName(command.userId(), command.initialDisplayName());

        AggregateLifecycle.apply(
                new ProfileEvent.ProfileCreatedEvent(
                        command.userId(),
                        command.email(),
                        command.initialDisplayName(),
                        Instant.now()
                )
        );
    }

    @CommandHandler
    public void update(ProfileCommand.UpdateProfileCommand command) {
        validateDisplayName(command.userId(), command.displayName());
        validateBio(command.userId(), command.bio());
        validateCountry(command.userId(), command.country());

        AggregateLifecycle.apply(
                new ProfileEvent.ProfileUpdatedEvent(
                        command.userId(),
                        command.requestedBy(),
                        command.displayName(),
                        command.bio(),
                        command.country(),
                        Instant.now()
                )
        );
    }

    @EventSourcingHandler
    public void on(ProfileEvent.ProfileCreatedEvent event) {
        this.userId = event.userId();
        this.email = event.email();
        this.displayName = event.displayName();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.ProfileUpdatedEvent event) {
        this.displayName = event.displayName();
        this.bio = event.bio();
        this.country = event.country();
    }

    private static void validateDisplayName(String userId, String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new ProfileProblems.DisplayNameBlankProblem(userId, displayName);
        }
        if (displayName.length() > ProfileRules.MAX_DISPLAY_NAME_LENGTH) {
            throw new ProfileProblems.DisplayNameTooLongProblem(userId, displayName);
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
}
