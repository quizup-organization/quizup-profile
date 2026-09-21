package io.github.quizup.profile.infrastructure.out.persistence.repository;

import io.github.quizup.profile.infrastructure.out.persistence.entity.ProgressionAwardedGameEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressionAwardedGameJpaRepository
        extends JpaRepository<ProgressionAwardedGameEntity, ProgressionAwardedGameEntity.AwardedGameId> {
}
