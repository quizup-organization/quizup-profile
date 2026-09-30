package io.github.quizup.profile.domain.event;

import io.github.quizup.microservice.core.domain.model.i18n.Language;

import java.time.Instant;

/**
 * Événements du profil : un événement par champ modifié.
 */
public interface ProfileEvent {

    String userId();

    /**
     * Événement émis lors de la création du profil (déclenchée par la saga
     * suite à un {@code UserRegisteredEvent} du service identity).
     */
    record ProfileCreatedEvent(
            String userId,
            String email,
            String pseudonym,
            Instant createdAt
    ) implements ProfileEvent {
    }

    record ProfilePseudonymUpdatedEvent(
            String userId,
            String requestedBy,
            String pseudonym,
            Instant updatedAt
    ) implements ProfileEvent {
    }

    record ProfileBioUpdatedEvent(
            String userId,
            String requestedBy,
            String bio,
            Instant updatedAt
    ) implements ProfileEvent {
    }

    record ProfileCountryUpdatedEvent(
            String userId,
            String requestedBy,
            String country,
            Instant updatedAt
    ) implements ProfileEvent {
    }

    record ProfileAvatarUpdatedEvent(
            String userId,
            String requestedBy,
            String avatarOptions,
            Instant updatedAt
    ) implements ProfileEvent {
    }

    record ProfileLanguageUpdatedEvent(
            String userId,
            String requestedBy,
            Language language,
            Instant updatedAt
    ) implements ProfileEvent {
    }
}
