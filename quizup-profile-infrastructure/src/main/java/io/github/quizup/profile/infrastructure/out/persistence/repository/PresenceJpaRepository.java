package io.github.quizup.profile.infrastructure.out.persistence.repository;

import io.github.quizup.profile.infrastructure.out.persistence.entity.PresenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PresenceJpaRepository
        extends JpaRepository<PresenceEntity, String>, JpaSpecificationExecutor<PresenceEntity> {
}
