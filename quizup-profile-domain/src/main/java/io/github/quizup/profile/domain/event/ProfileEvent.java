package io.github.quizup.profile.domain.event;

import java.time.Instant;

public interface ProfileEvent {
    String userId();

    /**
     * Événement émis lors de la création du profil (déclenchée par la saga
     * suite à un {@code UserRegisteredEvent} du service identity).
     */
    record ProfileCreatedEvent(
            String userId,
            String email,
            String displayName,
            Instant createdAt
    ) implements ProfileEvent {
    }

    /**
     * Événement émis lors d'une mise à jour du profil par son propriétaire.
     */
    record ProfileUpdatedEvent(
            String userId,
            String requestedBy,
            String displayName,
            String bio,
            String country,
            Instant updatedAt
    ) implements ProfileEvent {
    }
}
