package io.github.quizup.profile.infrastructure.out.persistence.repository;

import io.github.quizup.profile.infrastructure.out.persistence.entity.ActivityDayGameEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface ActivityDayGameJpaRepository
        extends JpaRepository<ActivityDayGameEntity, ActivityDayGameEntity.ActivityDayGameId> {

    long countByUserIdAndActivityDate(String userId, LocalDate activityDate);
}
