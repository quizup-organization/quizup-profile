package io.github.quizup.profile.infrastructure.in.scheduler;

import io.github.quizup.profile.domain.port.in.PresenceUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Confirme périodiquement les passages hors ligne arrivés à échéance (TTL en base). Un simple
 * balayage idempotent remplace les deadlines par déconnexion : rejouable, multi-instances et
 * indépendant de l'event bus.
 */
@Component
public class PresenceOfflineSweeper {

    private final PresenceUseCase presenceUseCase;

    public PresenceOfflineSweeper(PresenceUseCase presenceUseCase) {
        this.presenceUseCase = presenceUseCase;
    }

    @Scheduled(fixedDelayString = "${app.presence.sweep-interval-ms:5000}")
    public void sweep() {
        presenceUseCase.expireOfflineDeadlines();
    }
}
