package io.github.quizup.profile.domain.model;

/**
 * État de présence d'un joueur. Piloté par le cycle de vie des sessions temps réel : un joueur
 * est {@code ONLINE} tant qu'au moins une session STOMP est ouverte, et bascule {@code OFFLINE}
 * après le délai de grâce qui suit la fermeture de sa dernière session.
 */
public enum PresenceStatus {
    ONLINE,
    OFFLINE
}
