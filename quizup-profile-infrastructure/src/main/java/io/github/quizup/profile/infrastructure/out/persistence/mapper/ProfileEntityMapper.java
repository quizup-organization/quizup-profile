package io.github.quizup.profile.infrastructure.out.persistence.mapper;

import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.model.ProfileGame;
import io.github.quizup.profile.domain.model.ProfileTopic;
import io.github.quizup.profile.infrastructure.out.persistence.entity.GameResultEmbeddable;
import io.github.quizup.profile.infrastructure.out.persistence.entity.ProfileEntity;
import io.github.quizup.profile.infrastructure.out.persistence.entity.TopicStatisticsEmbeddable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ProfileEntityMapper {

    private ProfileEntityMapper() {
    }

    public static Profile toDomain(ProfileEntity entity) {
        Map<String, ProfileTopic> topics = new HashMap<>();
        entity.getTopics().forEach((topicId, value) ->
                topics.put(
                        topicId,
                        new ProfileTopic(
                                topicId,
                                value.getTotalExperience(),
                                value.getLevel(),
                                value.getWins(),
                                value.getLosses(),
                                value.getDraws(),
                                new ArrayList<>(),
                                value.getWinStreak(),
                                value.getDrawStreak(),
                                value.getLossStreak(),
                                value.getCreatedAt(),
                                value.getUpdatedAt()
                        )
                )
        );

        List<ProfileGame> games = entity.getGames().stream()
                .map(ProfileEntityMapper::toDomain)
                .toList();

        return new Profile(
                entity.getProfileId(),
                entity.getTotalExperience(),
                entity.getLevel(),
                entity.getWins(),
                entity.getLosses(),
                entity.getDraws(),
                entity.getWinStreak(),
                entity.getLossStreak(),
                entity.getDrawStreak(),
                topics,
                games,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static ProfileEntity toEntity(Profile profile) {
        ProfileEntity entity = new ProfileEntity();
        entity.setProfileId(profile.profileId());
        entity.setTotalExperience(profile.totalExperience());
        entity.setLevel(profile.level());
        entity.setWins(profile.wins());
        entity.setLosses(profile.losses());
        entity.setDraws(profile.draws());
        entity.setWinStreak(profile.winStreak());
        entity.setLossStreak(profile.lossStreak());
        entity.setDrawStreak(profile.drawStreak());

        Map<String, TopicStatisticsEmbeddable> topics = new HashMap<>();
        profile.topics().forEach((topicId, stats) -> {
            TopicStatisticsEmbeddable embeddable = new TopicStatisticsEmbeddable();
            embeddable.setTotalExperience(stats.totalExperience());
            embeddable.setLevel(stats.level());
            embeddable.setWins(stats.wins());
            embeddable.setLosses(stats.losses());
            embeddable.setDraws(stats.draws());
            embeddable.setWinStreak(stats.winStreak());
            embeddable.setLossStreak(stats.lossStreak());
            embeddable.setDrawStreak(stats.drawStreak());
            embeddable.setCreatedAt(stats.createdAt());
            embeddable.setUpdatedAt(stats.updatedAt());
            topics.put(topicId, embeddable);
        });
        entity.setTopics(topics);

        List<GameResultEmbeddable> games = profile.games().stream()
                .map(ProfileEntityMapper::toEntity)
                .toList();
        entity.setGames(games);

        entity.setCreatedAt(profile.createdAt());
        entity.setUpdatedAt(profile.updatedAt());
        return entity;
    }

    private static ProfileGame toDomain(GameResultEmbeddable embeddable) {
        return new ProfileGame(
                embeddable.getGameId(),
                embeddable.getTopicId(),
                embeddable.getOpponentId(),
                embeddable.getOpponentName(),
                embeddable.getPlayerScore(),
                embeddable.getOpponentScore(),
                embeddable.getResult(),
                embeddable.getPlayedAt()
        );
    }

    private static GameResultEmbeddable toEntity(ProfileGame gameResult) {
        GameResultEmbeddable embeddable = new GameResultEmbeddable();
        embeddable.setGameId(gameResult.gameId());
        embeddable.setTopicId(gameResult.topicId());
        embeddable.setOpponentId(gameResult.opponentId());
        embeddable.setOpponentName(gameResult.opponentName());
        embeddable.setPlayerScore(gameResult.playerScore());
        embeddable.setOpponentScore(gameResult.opponentScore());
        embeddable.setResult(gameResult.result());
        embeddable.setPlayedAt(gameResult.playedAt());
        return embeddable;
    }
}
