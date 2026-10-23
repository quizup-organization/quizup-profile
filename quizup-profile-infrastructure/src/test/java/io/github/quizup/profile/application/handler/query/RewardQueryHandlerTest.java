package io.github.quizup.profile.application.handler.query;

import io.github.quizup.profile.domain.model.PlayerRewards;
import io.github.quizup.profile.domain.model.RewardCode;
import io.github.quizup.profile.domain.port.out.PlayerRewardRepositoryPort;
import io.github.quizup.profile.domain.query.RewardQuery;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RewardQueryHandlerTest {

    private final PlayerRewardRepositoryPort repository = mock(PlayerRewardRepositoryPort.class);
    private final RewardQueryHandler handler = new RewardQueryHandler(repository);

    @Test
    void returnsPersistedRewards() {
        when(repository.findRewards("user-1")).thenReturn(Set.of(RewardCode.MICAH_AVATAR));

        PlayerRewards rewards = handler.handle(new RewardQuery.GetPlayerRewardsQuery("user-1"));

        assertThat(rewards.userId()).isEqualTo("user-1");
        assertThat(rewards.rewards()).containsExactly(RewardCode.MICAH_AVATAR);
    }
}
