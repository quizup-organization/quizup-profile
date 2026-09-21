package io.github.quizup.profile.infrastructure.out.persistence.adapter;

import io.github.quizup.profile.domain.port.out.ProgressionAwardedGameRepositoryPort;
import io.github.quizup.profile.infrastructure.out.persistence.entity.ProgressionAwardedGameEntity;
import io.github.quizup.profile.infrastructure.out.persistence.repository.ProgressionAwardedGameJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProgressionAwardedGameRepositoryAdapter implements ProgressionAwardedGameRepositoryPort {

    private final ProgressionAwardedGameJpaRepository repository;

    public ProgressionAwardedGameRepositoryAdapter(ProgressionAwardedGameJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public boolean record(String userId, String gameId) {
        ProgressionAwardedGameEntity.AwardedGameId id =
                new ProgressionAwardedGameEntity.AwardedGameId(userId, gameId);
        if (repository.existsById(id)) {
            return false;
        }
        repository.save(new ProgressionAwardedGameEntity(userId, gameId));
        return true;
    }
}
