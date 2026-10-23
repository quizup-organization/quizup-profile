package io.github.quizup.profile.application.projection;

import io.github.quizup.profile.domain.event.RewardEvent;
import io.github.quizup.profile.domain.port.out.PlayerRewardRepositoryPort;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Projette les récompenses débloquées dans {@code player_reward}. */
@Component
@ProcessingGroup("reward-projection")
public class PlayerRewardProjection {

    private final PlayerRewardRepositoryPort repositoryPort;

    public PlayerRewardProjection(PlayerRewardRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @EventHandler
    @Transactional
    public void on(RewardEvent.RewardGrantedEvent event) {
        repositoryPort.grant(event.userId(), event.rewardCode(), event.source(), event.unlockedAt());
    }
}
