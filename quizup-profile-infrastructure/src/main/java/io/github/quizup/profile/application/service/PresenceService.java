package io.github.quizup.profile.application.service;

import io.github.quizup.profile.domain.event.PresenceEvent;
import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.domain.model.PresenceDeadline;
import io.github.quizup.profile.domain.model.PresenceRules;
import io.github.quizup.profile.domain.model.PresenceStatus;
import io.github.quizup.profile.domain.port.in.PresenceUseCase;
import io.github.quizup.profile.domain.port.out.PresenceRepositoryPort;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.messaging.NoScopeDescriptor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Présence joueur pilotée par le cycle de vie de la session temps réel STOMP **du BFF**.
 *
 * <p>Le BFF (seule surface STOMP) signale les connexions/déconnexions via les commandes
 * {@code PresenceCommand}. Une session ouverte bascule le joueur {@code ONLINE} ; la fermeture
 * de la dernière session programme, après {@link PresenceRules#DISCONNECT_GRACE}, une échéance
 * de confirmation qui le bascule {@code OFFLINE} (une reconnexion entre-temps rend l'échéance
 * sans effet). Les transitions sont publiées comme événements ; le BFF en fait le fan-out STOMP.</p>
 */
@Service
public class PresenceService implements PresenceUseCase {

    private final PresenceRepositoryPort presenceRepositoryPort;
    private final EventGateway eventGateway;
    private final DeadlineManager deadlineManager;

    public PresenceService(PresenceRepositoryPort presenceRepositoryPort,
                           EventGateway eventGateway,
                           DeadlineManager deadlineManager) {
        this.presenceRepositoryPort = presenceRepositoryPort;
        this.eventGateway = eventGateway;
        this.deadlineManager = deadlineManager;
    }

    @Override
    public PlayerPresence sessionConnected(String sessionId, String userId) {
        Optional<PlayerPresence> existing = presenceRepositoryPort.findById(userId);
        boolean wasOnline = existing.map(PlayerPresence::isOnline).orElse(false);

        presenceRepositoryPort.addSession(sessionId, userId);

        Instant now = Instant.now();
        PlayerPresence presence = existing
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

        if (!wasOnline) {
            eventGateway.publish(new PresenceEvent.PlayerWentOnlineEvent(userId, now));
        }
        return saved;
    }

    @Override
    public PlayerPresence sessionDisconnected(String sessionId, String userId) {
        presenceRepositoryPort.removeSession(sessionId);

        if (presenceRepositoryPort.countSessions(userId) == 0) {
            // Dernière session fermée : on fige le « last seen », on reste ONLINE le temps de la
            // grâce, et on programme la confirmation du passage hors ligne.
            Instant now = Instant.now();
            presenceRepositoryPort.findById(userId).ifPresent(current ->
                    presenceRepositoryPort.save(current.toBuilder().lastSeenAt(now).build()));

            deadlineManager.schedule(
                    PresenceRules.DISCONNECT_GRACE,
                    PresenceDeadline.OFFLINE,
                    new PresenceDeadline.OfflineCheck(userId),
                    NoScopeDescriptor.INSTANCE
            );
        }

        return get(userId);
    }

    @Override
    public void confirmOffline(String userId) {
        if (presenceRepositoryPort.countSessions(userId) > 0) {
            return;
        }
        presenceRepositoryPort.findById(userId)
                .filter(PlayerPresence::isOnline)
                .ifPresent(presence -> {
                    PlayerPresence offline = presence.toBuilder()
                            .status(PresenceStatus.OFFLINE)
                            .build();
                    presenceRepositoryPort.save(offline);
                    eventGateway.publish(new PresenceEvent.PlayerWentOfflineEvent(
                            offline.userId(),
                            offline.lastSeenAt()
                    ));
                });
    }

    @Override
    public PlayerPresence get(String userId) {
        return presenceRepositoryPort.findById(userId)
                .orElseGet(() -> offline(userId));
    }

    private PlayerPresence offline(String userId) {
        return PlayerPresence.builder()
                .userId(userId)
                .status(PresenceStatus.OFFLINE)
                .lastSeenAt(null)
                .build();
    }
}
