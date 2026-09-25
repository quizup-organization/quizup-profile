package io.github.quizup.profile.domain.event;

import java.time.Instant;

/**
 * Événements de présence publiés sur le bus partagé (non event-sourcés). Ils permettent aux
 * autres services de réagir aux transitions — notamment le forfait d'un duel en cours.
 */
public interface PresenceEvent {

    String userId();

    /**
     * Un joueur vient de passer en ligne (première session temps réel ouverte).
     * Consommé par le BFF pour diffuser {@code /topic/presence/{userId}}.
     */
    record PlayerWentOnlineEvent(
            String userId,
            Instant at
    ) implements PresenceEvent {
    }

    /**
     * Un joueur vient de passer hors ligne (déconnexion confirmée après le délai de grâce).
     * Consommé par {@code quizup-game} pour clore par forfait un duel synchrone en cours,
     * et par le BFF pour diffuser {@code /topic/presence/{userId}}.
     */
    record PlayerWentOfflineEvent(
            String userId,
            Instant lastSeenAt
    ) implements PresenceEvent {
    }
}
