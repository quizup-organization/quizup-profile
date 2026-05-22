package io.github.quizup.profile.infrastructure.in.api.mapper;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.infrastructure.in.api.response.PageResponse;
import io.github.quizup.common.infrastructure.mapper.SearchResponseMapper;
import io.github.quizup.profile.domain.model.*;
import io.github.quizup.profile.infrastructure.in.api.response.*;

import java.util.Comparator;
import java.util.Map;

public final class ProfileResponseMapper {

    private ProfileResponseMapper() {
    }

    public static ProfileResponse toResponse(Profile profile) {
        return new ProfileResponse(
                profile.profileId(),
                profile.winStreak(),
                profile.lossStreak(),
                profile.drawStreak(),
                toStatisticsResponse(profile.globalStatistics()),
                profile.topicStatistics().entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> toStatisticsResponse(entry.getValue())
                        )),
                profile.badges().values().stream()
                        .sorted(Comparator.comparing(Badge::unlockedAt).reversed())
                        .map(ProfileResponseMapper::toResponse)
                        .toList(),
                profile.recentGameResults().stream()
                        .sorted(Comparator.comparing(GameResult::playedAt).reversed())
                        .map(ProfileResponseMapper::toResponse)
                        .toList(),
                profile.createdAt(),
                profile.updatedAt()
        );
    }

    public static PageResponse<ProfileResponse> toResponse(PageResult<Profile> pageResult) {
        return SearchResponseMapper.toSearchResponse(pageResult, ProfileResponseMapper::toResponse);
    }

    private static ProfileStatisticsResponse toStatisticsResponse(Statistics statistics) {
        return new ProfileStatisticsResponse(
                statistics.totalExperience(),
                statistics.level(),
                statistics.experience(),
                statistics.experienceAtCurrentLevel(),
                statistics.experienceAtNextLevel(),
                statistics.experienceRequiredToCompleteCurrentLevel(),
                statistics.wins(),
                statistics.losses(),
                statistics.draws(),
                statistics.totalGames(),
                statistics.winPercentage(),
                statistics.lossPercentage(),
                statistics.drawPercentage()
        );
    }

    private static BadgeResponse toResponse(Badge badge) {
        return new BadgeResponse(badge.type(), badge.unlockedAt());
    }

    private static GameResultResponse toResponse(GameResult gameResult) {
        return new GameResultResponse(
                gameResult.gameId(),
                gameResult.topicId(),
                gameResult.opponentId(),
                gameResult.playerScore(),
                gameResult.opponentScore(),
                gameResult.result(),
                gameResult.playedAt()
        );
    }
}

