package io.github.quizup.profile.domain.command;

import java.time.Instant;

/**
 * Commandes de présence : émises par le BFF (seule surface STOMP) pour signaler le cycle de vie
 * des sessions temps réel client. {@code instanceId} identifie l'instance BFF propriétaire des
 * sessions (purge ciblée au redémarrage).
 */
public interface PresenceCommand {

    record ConnectPlayerCommand(
            String sessionId,
            String userId,
            String instanceId
    ) implements PresenceCommand {
    }

    record DisconnectPlayerCommand(
            String sessionId,
            String userId
    ) implements PresenceCommand {
    }

    /**
     * Purge les sessions laissées par une incarnation précédente de l'instance BFF : seules les
     * sessions ouvertes **avant** son démarrage ({@code startedAt}) sont supprimées, pour ne pas
     * toucher aux connexions établies pendant la fenêtre de retry du démarrage.
     */
    record ResetInstanceSessionsCommand(
            String instanceId,
            Instant startedAt
    ) implements PresenceCommand {
    }
}
