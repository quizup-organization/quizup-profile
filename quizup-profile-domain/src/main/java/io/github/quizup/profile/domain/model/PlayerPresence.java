package io.github.quizup.profile.domain.model;

import lombok.Builder;

import java.time.Instant;

/**
 * Présence d'un joueur (read-model durable, non event-sourcé) : projection des transitions
 * {@code ONLINE}/{@code OFFLINE} dont la vérité vit dans les leases du store chaud. Sert aux
 * pastilles « en ligne » et au forfait des duels.
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
