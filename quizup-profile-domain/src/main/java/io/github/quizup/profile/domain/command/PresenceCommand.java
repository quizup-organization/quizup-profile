package io.github.quizup.profile.domain.command;

import java.time.Instant;
import java.util.List;

/**
 * Commandes de présence : émises par le BFF (seule surface STOMP) pour signaler le cycle de vie
 * des sessions temps réel client. {@code instanceId} identifie l'instance BFF propriétaire des
 * sessions.
 *
 * <p>Les leases vivent dans le store chaud (Redis) ; le joueur reste {@code ONLINE} tant que le
 * BFF renouvelle ses sessions ({@link RenewPresenceSessionsCommand}).</p>
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
     * Heartbeat batch d'une instance BFF : renouvelle les leases de ses sessions locales.
     * Une seule commande par intervalle de renouvellement, quel que soit le nombre de sessions.
     */
    record RenewPresenceSessionsCommand(
            List<String> sessionIds
    ) implements PresenceCommand {
    }

    /**
     * @deprecated Plus envoyée par le BFF : les leases TTL rendent la purge par instance inutile.
     * Conservée pour la compatibilité de rollout (ancien BFF), retirée au prochain lot.
     */
    @Deprecated
    record ResetInstanceSessionsCommand(
            String instanceId,
            Instant startedAt
    ) implements PresenceCommand {
    }
}
