package io.github.quizup.profile.infrastructure.out.persistence.adapter;

import io.github.quizup.profile.domain.model.GameXp;
import io.github.quizup.profile.domain.port.out.ProgressionAwardedGameRepositoryPort;
import io.github.quizup.profile.infrastructure.out.persistence.entity.ProgressionAwardedGameEntity;
import io.github.quizup.profile.infrastructure.out.persistence.repository.ProgressionAwardedGameJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ProgressionAwardedGameRepositoryAdapter implements ProgressionAwardedGameRepositoryPort {

    private final ProgressionAwardedGameJpaRepository repository;

    public ProgressionAwardedGameRepositoryAdapter(ProgressionAwardedGameJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public boolean record(String userId, String gameId, int xp) {
        ProgressionAwardedGameEntity.AwardedGameId id =
                new ProgressionAwardedGameEntity.AwardedGameId(userId, gameId);
        if (repository.existsById(id)) {
            return false;
        }
        repository.save(new ProgressionAwardedGameEntity(userId, gameId, xp));
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameXp> findGameXp(String userId, List<String> gameIds) {
        if (gameIds.isEmpty()) {
            return List.of();
        }
        return repository.findByUserIdAndGameIdIn(userId, gameIds).stream()
                .map(entity -> new GameXp(entity.getGameId(), entity.getXp()))
                .toList();
    }
}
