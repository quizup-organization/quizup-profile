package io.github.quizup.profile.infrastructure.out.messaging;

import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.domain.port.out.PresenceNotifierPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Diffuse les changements de présence sur {@code /topic/presence/{userId}} (STOMP via la gateway).
 */
@Component
public class PresenceNotifierAdapter implements PresenceNotifierPort {

    private static final String DESTINATION_PREFIX = "/topic/presence/";
    private static final Logger logger = LoggerFactory.getLogger(PresenceNotifierAdapter.class);

    private final SimpMessagingTemplate messagingTemplate;

    public PresenceNotifierAdapter(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publish(PlayerPresence presence) {
        logger.debug("Presence published: userId={}, status={}", presence.userId(), presence.status());
        messagingTemplate.convertAndSend(
                DESTINATION_PREFIX + presence.userId(),
                new PresenceNotification(presence.userId(), presence.status(), presence.lastSeenAt())
        );
    }
}
