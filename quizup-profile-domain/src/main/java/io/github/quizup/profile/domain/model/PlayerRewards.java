package io.github.quizup.profile.domain.model;

import java.util.Set;

/** Récompenses persistées d'un joueur. */
public record PlayerRewards(String userId, Set<RewardCode> rewards) {

    public PlayerRewards {
        rewards = rewards == null ? Set.of() : Set.copyOf(rewards);
    }

    public static PlayerRewards empty(String userId) {
        return new PlayerRewards(userId, Set.of());
    }

    public boolean has(RewardCode code) {
        return rewards.contains(code);
    }
}
