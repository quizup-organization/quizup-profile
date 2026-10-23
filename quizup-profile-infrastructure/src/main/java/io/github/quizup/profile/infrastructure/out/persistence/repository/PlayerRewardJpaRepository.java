package io.github.quizup.profile.infrastructure.out.persistence.repository;

import io.github.quizup.profile.infrastructure.out.persistence.entity.PlayerRewardEntity;
import io.github.quizup.profile.infrastructure.out.persistence.entity.PlayerRewardKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerRewardJpaRepository extends JpaRepository<PlayerRewardEntity, PlayerRewardKey> {

    List<PlayerRewardEntity> findByUserId(String userId);
}
