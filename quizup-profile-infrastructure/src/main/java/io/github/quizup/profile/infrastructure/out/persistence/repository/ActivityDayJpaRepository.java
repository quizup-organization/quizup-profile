package io.github.quizup.profile.infrastructure.out.persistence.repository;

import io.github.quizup.profile.infrastructure.out.persistence.entity.ActivityDayEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ActivityDayJpaRepository
        extends JpaRepository<ActivityDayEntity, ActivityDayEntity.ActivityDayId> {

    List<ActivityDayEntity> findByUserIdAndActivityDateBetweenOrderByActivityDateAsc(
            String userId, LocalDate from, LocalDate to);
}
