package io.github.quizup.profile.infrastructure.out.persistence.adapter;

import io.github.quizup.microservice.core.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.microservice.core.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.domain.port.out.PresenceRepositoryPort;
import io.github.quizup.profile.infrastructure.out.persistence.entity.PresenceEntity;
import io.github.quizup.profile.infrastructure.out.persistence.mapper.PresenceEntityMapper;
import io.github.quizup.profile.infrastructure.out.persistence.repository.PresenceJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class PresenceRepositoryAdapter implements PresenceRepositoryPort {

    private final PresenceJpaRepository presenceJpaRepository;
    private final JpaSearchAdapter<PresenceEntity> presenceJpaSearchAdapter;

    public PresenceRepositoryAdapter(PresenceJpaRepository presenceJpaRepository) {
        this.presenceJpaRepository = presenceJpaRepository;
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
}
