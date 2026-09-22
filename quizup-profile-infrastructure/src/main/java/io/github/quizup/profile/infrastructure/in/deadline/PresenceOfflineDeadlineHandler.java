package io.github.quizup.profile.infrastructure.in.deadline;

import io.github.quizup.profile.domain.model.PresenceDeadline;
import io.github.quizup.profile.domain.port.in.PresenceUseCase;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Confirme le passage hors ligne d'un joueur à l'échéance de grâce qui suit la fermeture de sa
 * dernière session. Si une reconnexion a eu lieu entre-temps, l'échéance est sans effet.
 *
 * <p>{@code DeadlineMessage} étendant {@code EventMessage}, Axon enregistre ce bean comme event
 * handler : il déclare donc son processing group (aucun {@code @EventHandler}, le handler est
 * déclenché par le {@code DeadlineManager}).</p>
 */
@Component
@ProcessingGroup("presence-offline-deadline")
public class PresenceOfflineDeadlineHandler {

    private static final Logger logger = LoggerFactory.getLogger(PresenceOfflineDeadlineHandler.class);

    private final PresenceUseCase presenceUseCase;

    public PresenceOfflineDeadlineHandler(PresenceUseCase presenceUseCase) {
        this.presenceUseCase = presenceUseCase;
    }

    @DeadlineHandler(deadlineName = PresenceDeadline.OFFLINE)
    public void onOfflineCheck(PresenceDeadline.OfflineCheck payload) {
        logger.debug("Presence offline grace elapsed: userId={}", payload.userId());
        presenceUseCase.confirmOffline(payload.userId());
    }
}
