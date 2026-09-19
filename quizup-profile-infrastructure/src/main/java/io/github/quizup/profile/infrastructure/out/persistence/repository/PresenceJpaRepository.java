package io.github.quizup.profile.infrastructure.out.persistence.repository;

import io.github.quizup.profile.domain.model.PresenceStatus;
import io.github.quizup.profile.infrastructure.out.persistence.entity.PresenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PresenceJpaRepository
        extends JpaRepository<PresenceEntity, String>, JpaSpecificationExecutor<PresenceEntity> {

    @Modifying
    @Query("update PresenceEntity p set p.status = :status")
    int updateStatusForAll(@Param("status") PresenceStatus status);
}
