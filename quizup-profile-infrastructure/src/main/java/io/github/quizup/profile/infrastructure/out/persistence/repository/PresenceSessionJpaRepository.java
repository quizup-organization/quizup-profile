package io.github.quizup.profile.infrastructure.out.persistence.repository;

import io.github.quizup.profile.infrastructure.out.persistence.entity.PresenceSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PresenceSessionJpaRepository extends JpaRepository<PresenceSessionEntity, String> {

    long countByUserId(String userId);

    @Query("""
            select distinct s.userId from PresenceSessionEntity s
            where s.instanceId = :instanceId
              and s.connectedAt < :before
            """)
    List<String> findUserIdsByInstanceBefore(@Param("instanceId") String instanceId,
                                             @Param("before") Instant before);

    void deleteByInstanceIdAndConnectedAtBefore(String instanceId, Instant before);

    @Modifying
    @Query("""
            update PresenceSessionEntity s
            set s.lastSeenAt = :now
            where s.sessionId in :sessionIds
            """)
    int touchSessions(@Param("sessionIds") List<String> sessionIds, @Param("now") Instant now);

    @Query("""
            select distinct s.userId from PresenceSessionEntity s
            where s.lastSeenAt < :before
            """)
    List<String> findStaleSessionUserIds(@Param("before") Instant before);

    void deleteByLastSeenAtBefore(Instant before);
}
