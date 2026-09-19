package io.github.quizup.profile.infrastructure.out.persistence.mapper;

import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.infrastructure.out.persistence.entity.ProgressEntity;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Mapper infrastructure — ProgressEntity (JPA) ⇄ PlayerProgress (domaine).
 */
public final class ProgressEntityMapper {

    private ProgressEntityMapper() {
    }

    public static PlayerProgress toDomain(ProgressEntity entity) {
        return PlayerProgress.builder()
                .userId(entity.getUserId())
                .xpTotal(entity.getXpTotal())
                .level(entity.getLevel())
                .title(entity.getTitle())
                .xpByTopic(new HashMap<>(entity.getXpByTopic()))
                .badges(new HashSet<>(entity.getBadges()))
                .gamesPlayed(entity.getGamesPlayed())
                .wins(entity.getWins())
                .losses(entity.getLosses())
                .bestScore(entity.getBestScore())
                .currentWinStreak(entity.getCurrentWinStreak())
                .bestWinStreak(entity.getBestWinStreak())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static ProgressEntity toEntity(PlayerProgress progress) {
        ProgressEntity entity = new ProgressEntity();
        entity.setUserId(progress.userId());
        entity.setXpTotal(progress.xpTotal());
        entity.setLevel(progress.level());
        entity.setTitle(progress.title());
        entity.setXpByTopic(new HashMap<>(progress.xpByTopic()));
        entity.setBadges(new HashSet<>(progress.badges()));
        entity.setGamesPlayed(progress.gamesPlayed());
        entity.setWins(progress.wins());
        entity.setLosses(progress.losses());
        entity.setBestScore(progress.bestScore());
        entity.setCurrentWinStreak(progress.currentWinStreak());
        entity.setBestWinStreak(progress.bestWinStreak());
        entity.setUpdatedAt(progress.updatedAt());
        return entity;
    }
}
