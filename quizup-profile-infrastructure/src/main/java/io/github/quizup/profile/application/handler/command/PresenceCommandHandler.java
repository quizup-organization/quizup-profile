package io.github.quizup.profile.application.handler.command;

import io.github.quizup.profile.domain.command.PresenceCommand;
import io.github.quizup.profile.domain.port.in.PresenceUseCase;
import org.axonframework.commandhandling.CommandHandler;
import org.springframework.stereotype.Component;

/**
 * Expose les transitions de session temps réel sur le bus de commandes distribué : le BFF
 * (seule surface STOMP) dispatche {@link PresenceCommand.ConnectPlayerCommand} /
 * {@link PresenceCommand.DisconnectPlayerCommand}.
 */
@Component
public class PresenceCommandHandler {

    private final PresenceUseCase presenceUseCase;

    public PresenceCommandHandler(PresenceUseCase presenceUseCase) {
        this.presenceUseCase = presenceUseCase;
    }

    @CommandHandler
    public void handle(PresenceCommand.ConnectPlayerCommand command) {
        presenceUseCase.sessionConnected(command.sessionId(), command.userId());
    }

    @CommandHandler
    public void handle(PresenceCommand.DisconnectPlayerCommand command) {
        presenceUseCase.sessionDisconnected(command.sessionId(), command.userId());
    }
}
