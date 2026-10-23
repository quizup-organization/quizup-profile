package io.github.quizup.profile.domain.port.out;

import io.github.quizup.profile.domain.model.RewardCode;
import io.github.quizup.profile.domain.model.RewardSource;

import java.time.Instant;
import java.util.Set;

/** Persistance de la projection des récompenses. */
public interface PlayerRewardRepositoryPort {

    void grant(String userId, RewardCode code, RewardSource source, Instant unlockedAt);

    Set<RewardCode> findRewards(String userId);
}
