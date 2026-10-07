package io.github.quizup.profile.infrastructure.out.persistence.repository;

import io.github.quizup.profile.infrastructure.out.persistence.entity.PresenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PresenceJpaRepository
        extends JpaRepository<PresenceEntity, String>, JpaSpecificationExecutor<PresenceEntity> {

    @Query("""
            select p from PresenceEntity p
            where p.status = io.github.quizup.profile.domain.model.PresenceStatus.ONLINE
              and p.offlineDeadlineAt is not null
              and p.offlineDeadlineAt <= :now
              and not exists (select s from PresenceSessionEntity s where s.userId = p.userId)
            """)
    List<PresenceEntity> findDueOffline(@Param("now") Instant now);

    /**
     * Transition conditionnelle : ne bascule que si l'échéance est toujours dépassée et qu'aucune
     * session n'a été rouverte entre-temps (idempotent, sûr entre instances).
     */
    @Modifying
    @Query("""
            update PresenceEntity p
            set p.status = io.github.quizup.profile.domain.model.PresenceStatus.OFFLINE,
                p.offlineDeadlineAt = null
            where p.userId = :userId
              and p.status = io.github.quizup.profile.domain.model.PresenceStatus.ONLINE
              and p.offlineDeadlineAt is not null
              and p.offlineDeadlineAt <= :now
              and not exists (select s from PresenceSessionEntity s where s.userId = p.userId)
            """)
    int markOffline(@Param("userId") String userId, @Param("now") Instant now);
}
