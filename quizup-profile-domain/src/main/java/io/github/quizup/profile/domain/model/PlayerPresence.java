package io.github.quizup.profile.domain.model;

import lombok.Builder;

import java.time.Instant;

/**
 * Présence d'un joueur (read-model éphémère, non event-sourcé). Pilotée par le cycle de vie des
 * sessions temps réel STOMP : un TTL {@code offlineDeadlineAt} est armé à la fermeture de la
 * dernière session et confirmé par un balayeur périodique. Sert aux pastilles « en ligne » et au
 * forfait des duels.
 */
@Builder(toBuilder = true)
public record PlayerPresence(
        String userId,
        PresenceStatus status,
        Instant lastSeenAt,
        Instant offlineDeadlineAt
) {
    public boolean isOnline() {
        return PresenceStatus.ONLINE == status;
    }
}
