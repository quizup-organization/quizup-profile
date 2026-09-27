package io.github.quizup.profile.infrastructure.out.persistence.repository;

import io.github.quizup.profile.infrastructure.out.persistence.entity.ProgressionAwardedGameEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProgressionAwardedGameJpaRepository
        extends JpaRepository<ProgressionAwardedGameEntity, ProgressionAwardedGameEntity.AwardedGameId> {

    List<ProgressionAwardedGameEntity> findByUserIdAndGameIdIn(String userId, Collection<String> gameIds);
}
