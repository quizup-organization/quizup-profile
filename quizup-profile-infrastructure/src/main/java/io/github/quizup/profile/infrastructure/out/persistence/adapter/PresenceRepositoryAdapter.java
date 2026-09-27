package io.github.quizup.profile.infrastructure.out.persistence.adapter;

import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.microservice.core.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.domain.port.out.PresenceRepositoryPort;
import io.github.quizup.profile.infrastructure.out.persistence.entity.PresenceEntity;
import io.github.quizup.profile.infrastructure.out.persistence.entity.PresenceSessionEntity;
import io.github.quizup.profile.infrastructure.out.persistence.mapper.PresenceEntityMapper;
import io.github.quizup.profile.infrastructure.out.persistence.repository.PresenceJpaRepository;
import io.github.quizup.profile.infrastructure.out.persistence.repository.PresenceSessionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class PresenceRepositoryAdapter implements PresenceRepositoryPort {

    private final PresenceJpaRepository presenceJpaRepository;
    private final PresenceSessionJpaRepository presenceSessionJpaRepository;
    private final JpaSearchAdapter<PresenceEntity> presenceJpaSearchAdapter;

    public PresenceRepositoryAdapter(PresenceJpaRepository presenceJpaRepository,
                                     PresenceSessionJpaRepository presenceSessionJpaRepository) {
        this.presenceJpaRepository = presenceJpaRepository;
        this.presenceSessionJpaRepository = presenceSessionJpaRepository;
        this.presenceJpaSearchAdapter =
                new JpaSearchAdapter<>(presenceJpaRepository, new AnnotationSearchableEntity(PresenceEntity.class));
    }

    @Override
    @Transactional
    public PlayerPresence save(PlayerPresence presence) {
        PresenceEntity saved = presenceJpaRepository.save(PresenceEntityMapper.toEntity(presence));
        return PresenceEntityMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlayerPresence> findById(String userId) {
        return presenceJpaRepository.findById(userId).map(PresenceEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResponse<PlayerPresence> findAll(SearchRequest request) {
        return presenceJpaSearchAdapter.findAll(request).map(PresenceEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerPresence> findByIds(List<String> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return presenceJpaRepository.findAllById(userIds).stream()
                .map(PresenceEntityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void addSession(String sessionId, String userId, String instanceId) {
        PresenceSessionEntity session = new PresenceSessionEntity();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setInstanceId(instanceId);
        session.setConnectedAt(Instant.now());
        presenceSessionJpaRepository.save(session);
    }

    @Override
    @Transactional
    public void removeSession(String sessionId) {
        presenceSessionJpaRepository.deleteById(sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countSessions(String userId) {
        return presenceSessionJpaRepository.countByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> userIdsByInstanceBefore(String instanceId, Instant before) {
        return presenceSessionJpaRepository.findUserIdsByInstanceBefore(instanceId, before);
    }

    @Override
    @Transactional
    public void deleteSessionsByInstanceBefore(String instanceId, Instant before) {
        presenceSessionJpaRepository.deleteByInstanceIdAndConnectedAtBefore(instanceId, before);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerPresence> findDueOffline(Instant now) {
        return presenceJpaRepository.findDueOffline(now).stream()
                .map(PresenceEntityMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public boolean markOffline(String userId, Instant now) {
        return presenceJpaRepository.markOffline(userId, now) == 1;
    }
}
