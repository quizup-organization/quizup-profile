package io.github.quizup.profile.infrastructure.out.persistence.adapter;

import io.github.quizup.profile.domain.model.RewardCode;
import io.github.quizup.profile.domain.model.RewardGrant;
import io.github.quizup.profile.domain.model.RewardSource;
import io.github.quizup.profile.domain.port.out.PlayerRewardRepositoryPort;
import io.github.quizup.profile.infrastructure.out.persistence.entity.PlayerRewardEntity;
import io.github.quizup.profile.infrastructure.out.persistence.entity.PlayerRewardKey;
import io.github.quizup.profile.infrastructure.out.persistence.repository.PlayerRewardJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Component
public class PlayerRewardRepositoryAdapter implements PlayerRewardRepositoryPort {

    private final PlayerRewardJpaRepository repository;

    public PlayerRewardRepositoryAdapter(PlayerRewardJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void grant(String userId, RewardCode code, RewardSource source, Instant unlockedAt) {
        PlayerRewardKey key = new PlayerRewardKey(userId, code);
        if (repository.existsById(key)) {
            return;
        }
        PlayerRewardEntity entity = new PlayerRewardEntity();
        entity.setUserId(userId);
        entity.setRewardCode(code);
        entity.setSource(source);
        entity.setUnlockedAt(unlockedAt);
        repository.save(entity);
    }

    @Override
    public List<RewardGrant> findRewards(String userId) {
        return repository.findByUserId(userId).stream()
                .map(entity -> new RewardGrant(
                        entity.getRewardCode(), entity.getSource(), entity.getUnlockedAt()))
                .sorted(Comparator.comparing(RewardGrant::unlockedAt).reversed())
                .toList();
    }
}
