package io.github.quizup.profile.infrastructure.in.deadline;

import io.github.quizup.profile.domain.model.PresenceDeadline;
import io.github.quizup.profile.domain.port.in.PresenceUseCase;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Confirme le passage hors ligne d'un joueur à l'échéance de grâce qui suit la fermeture de sa
 * dernière session. Si une reconnexion a eu lieu entre-temps, l'échéance est sans effet.
 */
@Component
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
