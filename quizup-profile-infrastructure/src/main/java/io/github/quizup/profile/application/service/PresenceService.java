package io.github.quizup.profile.application.service;

import io.github.quizup.profile.domain.event.PresenceEvent;
import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.domain.model.PresenceDeadline;
import io.github.quizup.profile.domain.model.PresenceRules;
import io.github.quizup.profile.domain.model.PresenceStatus;
import io.github.quizup.profile.domain.port.in.PresenceUseCase;
import io.github.quizup.profile.domain.port.out.PresenceNotifierPort;
import io.github.quizup.profile.domain.port.out.PresenceRepositoryPort;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Présence joueur pilotée par le cycle de vie de la session temps réel STOMP.
 *
 * <p>Une session ouverte bascule le joueur {@code ONLINE} ; la fermeture de la dernière
 * session programme, après {@link PresenceRules#DISCONNECT_GRACE}, une échéance de
 * confirmation qui le bascule {@code OFFLINE} (une reconnexion entre-temps rend l'échéance
 * sans effet). Aucun battement de cœur ni polling : la seule source est la session, et la
 * notification temps réel n'est diffusée qu'au changement d'état.</p>
 */
@Service
public class PresenceService implements PresenceUseCase {

    private final PresenceRepositoryPort presenceRepositoryPort;
    private final PresenceNotifierPort presenceNotifierPort;
    private final EventGateway eventGateway;
    private final DeadlineManager deadlineManager;

    public PresenceService(PresenceRepositoryPort presenceRepositoryPort,
                           PresenceNotifierPort presenceNotifierPort,
                           EventGateway eventGateway,
                           DeadlineManager deadlineManager) {
        this.presenceRepositoryPort = presenceRepositoryPort;
        this.presenceNotifierPort = presenceNotifierPort;
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
            presenceNotifierPort.publish(saved);
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
                    new PresenceDeadline.OfflineCheck(userId)
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
                    presenceNotifierPort.publish(offline);
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
