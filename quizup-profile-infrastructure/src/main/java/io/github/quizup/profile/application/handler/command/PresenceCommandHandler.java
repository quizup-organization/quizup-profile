package io.github.quizup.profile.application.handler.command;

import io.github.quizup.profile.domain.command.PresenceCommand;
import io.github.quizup.profile.domain.port.in.PresenceUseCase;
import org.axonframework.commandhandling.CommandHandler;
import org.springframework.stereotype.Component;

/**
 * Expose le cycle de vie des sessions temps réel sur le bus de commandes distribué : le BFF
 * (seule surface STOMP) dispatche {@link PresenceCommand.ConnectPlayerCommand} /
 * {@link PresenceCommand.DisconnectPlayerCommand} et le heartbeat batch
 * {@link PresenceCommand.RenewPresenceSessionsCommand}.
 */
@Component
public class PresenceCommandHandler {

    private final PresenceUseCase presenceUseCase;

    public PresenceCommandHandler(PresenceUseCase presenceUseCase) {
        this.presenceUseCase = presenceUseCase;
    }

    @CommandHandler
    public void handle(PresenceCommand.ConnectPlayerCommand command) {
        presenceUseCase.sessionConnected(command.sessionId(), command.userId(), command.instanceId());
    }

    @CommandHandler
    public void handle(PresenceCommand.DisconnectPlayerCommand command) {
        presenceUseCase.sessionDisconnected(command.sessionId(), command.userId());
    }

    @CommandHandler
    public void handle(PresenceCommand.RenewPresenceSessionsCommand command) {
        presenceUseCase.renewSessions(command.sessionIds());
    }

    /**
     * @deprecated Plus envoyée par le BFF (leases TTL) ; handler no-op conservé le temps du
     * rollout pour les anciennes instances BFF encore en service.
     */
    @Deprecated
    @CommandHandler
    public void handle(PresenceCommand.ResetInstanceSessionsCommand command) {
        // no-op volontaire
    }
}
