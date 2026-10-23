package io.github.quizup.profile.infrastructure.out.persistence.entity;

import io.github.quizup.profile.domain.model.RewardCode;

import java.io.Serializable;
import java.util.Objects;

/** Clé composite de {@link PlayerRewardEntity}. */
public class PlayerRewardKey implements Serializable {

    private String userId;
    private RewardCode rewardCode;

    public PlayerRewardKey() {
    }

    public PlayerRewardKey(String userId, RewardCode rewardCode) {
        this.userId = userId;
        this.rewardCode = rewardCode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public RewardCode getRewardCode() {
        return rewardCode;
    }

    public void setRewardCode(RewardCode rewardCode) {
        this.rewardCode = rewardCode;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PlayerRewardKey that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && rewardCode == that.rewardCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, rewardCode);
    }
}
