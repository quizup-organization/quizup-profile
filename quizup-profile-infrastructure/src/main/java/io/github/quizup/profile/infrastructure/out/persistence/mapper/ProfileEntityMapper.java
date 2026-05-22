package io.github.quizup.profile.infrastructure.out.persistence.mapper;

import io.github.quizup.profile.domain.model.*;
import io.github.quizup.profile.infrastructure.out.persistence.entity.*;

import java.util.*;

public final class ProfileEntityMapper {

    private ProfileEntityMapper() {
    }

    public static Profile toDomain(ProfileEntity entity) {
        Map<String, TopicStatistics> topicStatistics = new HashMap<>();
        entity.getTopicStatistics().forEach((topicId, value) ->
                topicStatistics.put(
                        topicId,
                        new TopicStatistics(topicId, value.getTotalExperience(), value.getWins(), value.getLosses(), value.getDraws(), value.getWinStreak())
                )
        );

        Map<BadgeType, Badge> badges = new EnumMap<>(BadgeType.class);
        entity.getBadges().forEach((badgeType, value) -> badges.put(badgeType, new Badge(badgeType, value.getUnlockedAt())));

        List<GameResult> recentGameResults = entity.getRecentGameResults().stream()
                .map(ProfileEntityMapper::toDomain)
                .toList();

        return new Profile(
                entity.getProfileId(),
                entity.getWinStreak(),
                entity.getLossStreak(),
                entity.getDrawStreak(),
                new GlobalStatistics(
                        entity.getGlobalTotalExperience(),
                        entity.getGlobalWins(),
                        entity.getGlobalLosses(),
                        entity.getGlobalDraws()
                ),
                topicStatistics,
                badges,
                recentGameResults,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static ProfileEntity toEntity(Profile profile) {
        ProfileEntity entity = new ProfileEntity();
        entity.setProfileId(profile.profileId());
        entity.setWinStreak(profile.winStreak());
        entity.setLossStreak(profile.lossStreak());
        entity.setDrawStreak(profile.drawStreak());
        entity.setGlobalTotalExperience(profile.globalStatistics().totalExperience());
        entity.setGlobalWins(profile.globalStatistics().wins());
        entity.setGlobalLosses(profile.globalStatistics().losses());
        entity.setGlobalDraws(profile.globalStatistics().draws());

        Map<String, TopicStatisticsEmbeddable> topicStatistics = new HashMap<>();
        profile.topicStatistics().forEach((topicId, stats) -> {
            TopicStatisticsEmbeddable embeddable = new TopicStatisticsEmbeddable();
            embeddable.setTotalExperience(stats.totalExperience());
            embeddable.setWins(stats.wins());
            embeddable.setLosses(stats.losses());
            embeddable.setDraws(stats.draws());
            embeddable.setWinStreak(stats.winStreak());
            topicStatistics.put(topicId, embeddable);
        });
        entity.setTopicStatistics(topicStatistics);

        Map<BadgeType, BadgeEmbeddable> badges = new EnumMap<>(BadgeType.class);
        profile.badges().forEach((type, badge) -> {
            BadgeEmbeddable embeddable = new BadgeEmbeddable();
            embeddable.setUnlockedAt(badge.unlockedAt());
            badges.put(type, embeddable);
        });
        entity.setBadges(badges);

        List<GameResultEmbeddable> results = profile.recentGameResults().stream()
                .map(ProfileEntityMapper::toEntity)
                .toList();
        entity.setRecentGameResults(results);

        entity.setCreatedAt(profile.createdAt());
        entity.setUpdatedAt(profile.updatedAt());
        return entity;
    }

    private static GameResult toDomain(GameResultEmbeddable embeddable) {
        return new GameResult(
                embeddable.getGameId(),
                embeddable.getTopicId(),
                embeddable.getOpponentId(),
                embeddable.getPlayerScore(),
                embeddable.getOpponentScore(),
                embeddable.getResult(),
                embeddable.getPlayedAt()
        );
    }

    private static GameResultEmbeddable toEntity(GameResult gameResult) {
        GameResultEmbeddable embeddable = new GameResultEmbeddable();
        embeddable.setGameId(gameResult.gameId());
        embeddable.setTopicId(gameResult.topicId());
        embeddable.setOpponentId(gameResult.opponentId());
        embeddable.setPlayerScore(gameResult.playerScore());
        embeddable.setOpponentScore(gameResult.opponentScore());
        embeddable.setResult(gameResult.result());
        embeddable.setPlayedAt(gameResult.playedAt());
        return embeddable;
    }
}

