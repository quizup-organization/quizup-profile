package io.github.quizup.profile.infrastructure.out.persistence.adapter;

import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.port.out.ProgressionRepositoryPort;
import io.github.quizup.profile.infrastructure.out.persistence.mapper.ProgressEntityMapper;
import io.github.quizup.profile.infrastructure.out.persistence.repository.ProgressJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProgressionRepositoryAdapter implements ProgressionRepositoryPort {

    private final ProgressJpaRepository progressJpaRepository;

    public ProgressionRepositoryAdapter(ProgressJpaRepository progressJpaRepository) {
        this.progressJpaRepository = progressJpaRepository;
    }

    @Override
    public void save(PlayerProgress progress) {
        progressJpaRepository.save(ProgressEntityMapper.toEntity(progress));
    }

    @Override
    public Optional<PlayerProgress> findById(String userId) {
        return progressJpaRepository.findById(userId).map(ProgressEntityMapper::toDomain);
    }
}
