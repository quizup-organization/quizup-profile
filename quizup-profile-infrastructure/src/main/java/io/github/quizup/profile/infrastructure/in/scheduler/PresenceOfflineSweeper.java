package io.github.quizup.profile.infrastructure.in.scheduler;

import io.github.quizup.profile.domain.port.in.PresenceUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Balayage périodique de la présence (TTL en base) :
 * <ol>
 *   <li>supprime les sessions dont le bail ({@code last_seen_at}) n'a pas été renouvelé par le
 *       BFF depuis {@code SESSION_LEASE_TTL} (instance disparue, client mort) ;</li>
 *   <li>confirme les passages hors ligne arrivés à échéance de grâce (aucune session restante)
 *       et publie les événements correspondants.</li>
 * </ol>
 * Simple balayage idempotent, rejouable, multi-instances et indépendant de l'event bus.
 */
@Component
public class PresenceOfflineSweeper {

    private final PresenceUseCase presenceUseCase;

    public PresenceOfflineSweeper(PresenceUseCase presenceUseCase) {
        this.presenceUseCase = presenceUseCase;
    }

    @Scheduled(fixedDelayString = "${app.presence.sweep-interval-ms:5000}")
    public void sweep() {
        presenceUseCase.expireStaleSessions();
        presenceUseCase.expireOfflineDeadlines();
    }
}
