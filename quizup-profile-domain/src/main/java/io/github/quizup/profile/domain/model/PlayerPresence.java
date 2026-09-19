package io.github.quizup.profile.domain.model;

import lombok.Builder;

import java.time.Instant;

/**
 * Présence d'un joueur (read-model éphémère, non event-sourcé). Alimentée par les
 * battements de cœur du client ; sert aux pastilles « en ligne » et au forfait des duels.
 */
@Builder(toBuilder = true)
public record PlayerPresence(
        String userId,
        PresenceStatus status,
        Instant lastSeenAt
) {
    public boolean isOnline() {
        return PresenceStatus.ONLINE == status;
    }
}
