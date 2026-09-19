package io.github.quizup.profile.domain.event;

import java.time.Instant;

/**
 * Événements de présence publiés sur le bus partagé (non event-sourcés). Ils permettent aux
 * autres services de réagir aux transitions — notamment le forfait d'un duel en cours.
 */
public interface PresenceEvent {

    String userId();

    /**
     * Un joueur vient de passer hors ligne (battement de cœur trop ancien). Consommé par
     * {@code quizup-game} pour clore par forfait un duel synchrone en cours.
     */
    record PlayerWentOfflineEvent(
            String userId,
            Instant lastSeenAt
    ) implements PresenceEvent {
    }
}
