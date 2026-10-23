package io.github.quizup.profile.infrastructure.out.persistence.entity;

import io.github.quizup.profile.domain.model.RewardCode;
import io.github.quizup.profile.domain.model.RewardSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Projection JPA d'une récompense débloquée par un joueur. */
@Getter
@Setter
@Entity
@Table(name = "player_reward")
@IdClass(PlayerRewardKey.class)
public class PlayerRewardEntity {

    @Id
    @Column(name = "user_id", length = 255, nullable = false)
    private String userId;

    @Id
    @Column(name = "reward_code", length = 64, nullable = false)
    @Enumerated(EnumType.STRING)
    private RewardCode rewardCode;

    @Column(name = "source", length = 32, nullable = false)
    @Enumerated(EnumType.STRING)
    private RewardSource source;

    @Column(name = "unlocked_at", nullable = false)
    private Instant unlockedAt;
}
