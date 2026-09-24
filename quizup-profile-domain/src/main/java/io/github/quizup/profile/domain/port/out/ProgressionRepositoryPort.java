package io.github.quizup.profile.domain.port.out;

import io.github.quizup.profile.domain.model.PlayerProgress;

import java.util.List;
import java.util.Optional;

/**
 * Port sortant — persistance de la projection de progression.
 */
public interface ProgressionRepositoryPort {

    void save(PlayerProgress progress);

    Optional<PlayerProgress> findById(String userId);

    /**
     * Trouve plusieurs progressions par identifiants (résolution en lot).
     */
    List<PlayerProgress> findByIds(List<String> userIds);
}
