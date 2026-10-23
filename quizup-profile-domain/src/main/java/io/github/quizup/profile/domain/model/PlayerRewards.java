package io.github.quizup.profile.domain.model;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Récompenses persistées d'un joueur. */
public record PlayerRewards(String userId, List<RewardGrant> rewards) {

    public PlayerRewards {
        rewards = rewards == null ? List.of() : List.copyOf(rewards);
    }

    public static PlayerRewards empty(String userId) {
        return new PlayerRewards(userId, List.of());
    }

    public boolean has(RewardCode code) {
        return rewards.stream().anyMatch(reward -> reward.code() == code);
    }

    public Set<RewardCode> codes() {
        return rewards.stream().map(RewardGrant::code).collect(Collectors.toUnmodifiableSet());
    }
}
