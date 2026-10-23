package io.github.quizup.profile.application.handler.query;

import io.github.quizup.profile.domain.model.PlayerRewards;
import io.github.quizup.profile.domain.port.out.PlayerRewardRepositoryPort;
import io.github.quizup.profile.domain.query.RewardQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

/** Handler Axon des récompenses persistées. */
@Component
public class RewardQueryHandler {

    private final PlayerRewardRepositoryPort repositoryPort;

    public RewardQueryHandler(PlayerRewardRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @QueryHandler
    public PlayerRewards handle(RewardQuery.GetPlayerRewardsQuery query) {
        return new PlayerRewards(query.userId(), repositoryPort.findRewards(query.userId()));
    }
}
