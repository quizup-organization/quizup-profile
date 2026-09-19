package io.github.quizup.profile.domain.port.out;

import io.github.quizup.profile.domain.model.PlayerPresence;

/**
 * Diffusion temps réel des changements de présence (WebSocket, destination
 * {@code /topic/presence/{userId}}). Implémenté en infrastructure.
 */
public interface PresenceNotifierPort {

    void publish(PlayerPresence presence);
}
