package io.github.quizup.profile.infrastructure.out.persistence.repository;

import io.github.quizup.profile.infrastructure.out.persistence.entity.PresenceSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresenceSessionJpaRepository extends JpaRepository<PresenceSessionEntity, String> {

    long countByUserId(String userId);
}
