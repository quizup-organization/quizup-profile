package io.github.quizup.profile.application.service;

import io.github.quizup.profile.domain.event.PresenceEvent;
import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.domain.model.PresenceStatus;
import io.github.quizup.profile.domain.port.in.PresenceUseCase;
import io.github.quizup.profile.domain.port.out.PresenceLeasePort;
import io.github.quizup.profile.domain.port.out.PresenceRepositoryPort;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Présence joueur adossée à des <b>leases de session</b> dans le store chaud (Redis, source de
 * vérité) et à une projection durable en base.
 *
 * <p>Le BFF (seule surface STOMP) signale les connexions/déconnexions et renouvelle les leases
 * de ses sessions locales (heartbeat batch). La fermeture de la dernière session arme la grâce
 * de déconnexion ; un lease non renouvelé expire (crash, coupure half-open). Le balayeur
 * périodique réclame les échéances dépassées sans session vivante et publie
 * {@code PlayerWentOfflineEvent} — idempotent et sûr entre instances.</p>
 */
@Service
public class PresenceService implements PresenceUseCase {

    private static final Logger logger = LoggerFactory.getLogger(PresenceService.class);

    private final PresenceRepositoryPort presenceRepositoryPort;
    private final PresenceLeasePort presenceLeasePort;
    private final EventGateway eventGateway;

    public PresenceService(PresenceRepositoryPort presenceRepositoryPort,
                           PresenceLeasePort presenceLeasePort,
                           EventGateway eventGateway) {
        this.presenceRepositoryPort = presenceRepositoryPort;
        this.presenceLeasePort = presenceLeasePort;
        this.eventGateway = eventGateway;
    }

    @Override
    public PlayerPresence sessionConnected(String sessionId, String userId, String instanceId) {
        boolean becameOnline = presenceLeasePort.openSession(userId, sessionId);
        if (!becameOnline) {
            return get(userId);
        }

        Instant now = Instant.now();
        PlayerPresence presence = presenceRepositoryPort.findById(userId)
                .map(current -> current.toBuilder()
                        .status(PresenceStatus.ONLINE)
                        .lastSeenAt(now)
                        .build())
                .orElseGet(() -> PlayerPresence.builder()
                        .userId(userId)
                        .status(PresenceStatus.ONLINE)
                        .lastSeenAt(now)
                        .build());
        PlayerPresence saved = presenceRepositoryPort.save(presence);

        eventGateway.publish(new PresenceEvent.PlayerWentOnlineEvent(userId, now));
        return saved;
    }

    @Override
    public PlayerPresence sessionDisconnected(String sessionId, String userId) {
        boolean stillAlive = presenceLeasePort.closeSession(userId, sessionId);
        if (!stillAlive) {
            // Dernière session fermée : on fige le « last seen » ; le balayeur confirmera le
            // passage hors ligne si aucune reconnexion n'intervient pendant la grâce.
            Instant now = Instant.now();
            presenceRepositoryPort.findById(userId).ifPresent(current ->
                    presenceRepositoryPort.save(current.toBuilder().lastSeenAt(now).build()));
        }
        return get(userId);
    }

    @Override
    public PlayerPresence get(String userId) {
        return presenceRepositoryPort.findById(userId)
                .orElseGet(() -> offline(userId));
    }

    @Override
    public void renewSessions(List<String> sessionIds) {
        presenceLeasePort.renewSessions(sessionIds);
    }

    @Override
    public void expireOfflineDeadlines() {
        for (String userId : presenceLeasePort.claimDueOffline(Instant.now())) {
            presenceRepositoryPort.findById(userId).ifPresentOrElse(current -> {
                presenceRepositoryPort.save(current.toBuilder()
                        .status(PresenceStatus.OFFLINE)
                        .build());
                eventGateway.publish(new PresenceEvent.PlayerWentOfflineEvent(
                        userId,
                        current.lastSeenAt() != null ? current.lastSeenAt() : Instant.now()));
            }, () -> logger.warn("Passage hors ligne réclamé pour un joueur sans projection: userId={}", userId));
        }
    }

    private static PlayerPresence offline(String userId) {
        return PlayerPresence.builder()
                .userId(userId)
                .status(PresenceStatus.OFFLINE)
                .lastSeenAt(null)
                .build();
    }
}
