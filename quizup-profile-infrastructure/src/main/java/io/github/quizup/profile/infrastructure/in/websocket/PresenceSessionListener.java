package io.github.quizup.profile.infrastructure.in.websocket;

import io.github.quizup.profile.domain.port.in.PresenceUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * Adaptateur temps réel : traduit le cycle de vie d'une session STOMP (connexion/déconnexion)
 * en cas d'usage de présence. Le {@code Principal} est posé par le
 * {@code StompAuthChannelInterceptor} du SDK (nom = claim {@code user_id}).
 */
@Component
public class PresenceSessionListener {

    private static final Logger logger = LoggerFactory.getLogger(PresenceSessionListener.class);

    private final PresenceUseCase presenceUseCase;

    public PresenceSessionListener(PresenceUseCase presenceUseCase) {
        this.presenceUseCase = presenceUseCase;
    }

    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        Principal user = event.getUser();
        String sessionId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
        if (user == null || sessionId == null) {
            return;
        }
        logger.debug("Presence session connected: userId={}, sessionId={}", user.getName(), sessionId);
        presenceUseCase.sessionConnected(sessionId, user.getName());
    }

    @EventListener
    public void onSessionDisconnected(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        String sessionId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
        if (user == null || sessionId == null) {
            return;
        }
        logger.debug("Presence session disconnected: userId={}, sessionId={}", user.getName(), sessionId);
        presenceUseCase.sessionDisconnected(sessionId, user.getName());
    }
}
