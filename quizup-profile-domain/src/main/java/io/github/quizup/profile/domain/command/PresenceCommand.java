package io.github.quizup.profile.domain.command;

/**
 * Commandes de présence : émises par le BFF (seule surface STOMP) pour signaler
 * l'ouverture/fermeture d'une session temps réel client.
 */
public interface PresenceCommand {

    record ConnectPlayerCommand(
            String sessionId,
            String userId
    ) implements PresenceCommand {
    }

    record DisconnectPlayerCommand(
            String sessionId,
            String userId
    ) implements PresenceCommand {
    }
}
