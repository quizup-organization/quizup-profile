package io.github.quizup.profile.application.service;

import io.github.quizup.profile.domain.event.PresenceEvent;
import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.domain.model.PresenceRules;
import io.github.quizup.profile.domain.model.PresenceStatus;
import io.github.quizup.profile.domain.port.in.PresenceUseCase;
import io.github.quizup.profile.domain.port.out.PresenceRepositoryPort;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Présence joueur pilotée par le cycle de vie de la session temps réel STOMP **du BFF**.
 *
 * <p>Le BFF (seule surface STOMP) signale les connexions/déconnexions via les commandes
 * {@code PresenceCommand}. Une session ouverte bascule le joueur {@code ONLINE} ; la fermeture
 * de la dernière session arme une échéance de grâce en base ({@code offline_deadline_at}),
 * confirmée par le balayeur périodique : plus de deadline Axon (le délai est un simple TTL,
 * rejouable et multi-instances).</p>
 */
@Service
public class PresenceService implements PresenceUseCase {

    private final PresenceRepositoryPort presenceRepositoryPort;
    private final EventGateway eventGateway;

    public PresenceService(PresenceRepositoryPort presenceRepositoryPort,
                           EventGateway eventGateway) {
        this.presenceRepositoryPort = presenceRepositoryPort;
        this.eventGateway = eventGateway;
    }

    @Override
    public PlayerPresence sessionConnected(String sessionId, String userId, String instanceId) {
        Optional<PlayerPresence> existing = presenceRepositoryPort.findById(userId);
        boolean wasOnline = existing.map(PlayerPresence::isOnline).orElse(false);

        presenceRepositoryPort.addSession(sessionId, userId, instanceId);

        Instant now = Instant.now();
        PlayerPresence presence = existing
                .map(current -> current.toBuilder()
                        .status(PresenceStatus.ONLINE)
                        .lastSeenAt(now)
                        .offlineDeadlineAt(null)
                        .build())
                .orElseGet(() -> PlayerPresence.builder()
                        .userId(userId)
                        .status(PresenceStatus.ONLINE)
                        .lastSeenAt(now)
                        .offlineDeadlineAt(null)
                        .build());

        PlayerPresence saved = presenceRepositoryPort.save(presence);

        if (!wasOnline) {
            eventGateway.publish(new PresenceEvent.PlayerWentOnlineEvent(userId, now));
        }
        return saved;
    }

    @Override
    public PlayerPresence sessionDisconnected(String sessionId, String userId) {
        presenceRepositoryPort.removeSession(sessionId);

        if (presenceRepositoryPort.countSessions(userId) == 0) {
            // Dernière session fermée : on fige le « last seen » et on arme l'échéance de grâce ;
            // le balayeur confirmera le passage hors ligne si aucune reconnexion n'intervient.
            Instant now = Instant.now();
            presenceRepositoryPort.findById(userId).ifPresent(current ->
                    presenceRepositoryPort.save(current.toBuilder()
                            .lastSeenAt(now)
                            .offlineDeadlineAt(now.plus(PresenceRules.DISCONNECT_GRACE))
                            .build()));
        }

        return get(userId);
    }

    @Override
    public PlayerPresence get(String userId) {
        return presenceRepositoryPort.findById(userId)
                .orElseGet(() -> offline(userId));
    }

    @Override
    public void resetInstanceSessions(String instanceId, Instant startedAt) {
        List<String> affectedUsers = presenceRepositoryPort.userIdsByInstanceBefore(instanceId, startedAt);
        presenceRepositoryPort.deleteSessionsByInstanceBefore(instanceId, startedAt);

        Instant now = Instant.now();
        affectedUsers.stream()
                .filter(userId -> presenceRepositoryPort.countSessions(userId) == 0)
                .forEach(userId -> presenceRepositoryPort.findById(userId).ifPresent(current ->
                        presenceRepositoryPort.save(current.toBuilder()
                                .lastSeenAt(now)
                                .offlineDeadlineAt(now.plus(PresenceRules.DISCONNECT_GRACE))
                                .build())));
    }

    @Override
    public void renewSessions(List<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }
        presenceRepositoryPort.touchSessions(sessionIds, Instant.now());
    }

    @Override
    public void expireStaleSessions() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(PresenceRules.SESSION_LEASE_TTL);
        List<String> affectedUsers = presenceRepositoryPort.findStaleSessionUserIds(cutoff);
        if (affectedUsers.isEmpty()) {
            return;
        }
        presenceRepositoryPort.deleteStaleSessions(cutoff);

        Instant deadline = now.plus(PresenceRules.DISCONNECT_GRACE);
        affectedUsers.stream()
                .filter(userId -> presenceRepositoryPort.countSessions(userId) == 0)
                .forEach(userId -> presenceRepositoryPort.findById(userId)
                        .filter(PlayerPresence::isOnline)
                        .ifPresent(current -> presenceRepositoryPort.save(current.toBuilder()
                                .offlineDeadlineAt(deadline)
                                .build())));
    }

    @Override
    public void expireOfflineDeadlines() {
        Instant now = Instant.now();
        presenceRepositoryPort.findDueOffline(now).forEach(presence -> {
            if (presenceRepositoryPort.markOffline(presence.userId(), now)) {
                eventGateway.publish(new PresenceEvent.PlayerWentOfflineEvent(
                        presence.userId(),
                        presence.lastSeenAt()
                ));
            }
        });
    }

    private PlayerPresence offline(String userId) {
        return PlayerPresence.builder()
                .userId(userId)
                .status(PresenceStatus.OFFLINE)
                .lastSeenAt(null)
                .offlineDeadlineAt(null)
                .build();
    }
}
