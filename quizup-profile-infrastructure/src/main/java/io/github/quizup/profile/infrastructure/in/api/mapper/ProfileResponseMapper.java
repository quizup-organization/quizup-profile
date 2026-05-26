package io.github.quizup.profile.infrastructure.in.api.mapper;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.infrastructure.in.api.response.PageResponse;
import io.github.quizup.common.infrastructure.mapper.SearchResponseMapper;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.model.ProfileGame;
import io.github.quizup.profile.domain.model.Statistics;
import io.github.quizup.profile.infrastructure.in.api.response.GameResultResponse;
import io.github.quizup.profile.infrastructure.in.api.response.ProfileResponse;
import io.github.quizup.profile.infrastructure.in.api.response.ProfileStatisticsResponse;

import java.util.Comparator;
import java.util.Map;

public final class ProfileResponseMapper {

    private ProfileResponseMapper() {
    }

    public static ProfileResponse toResponse(Profile profile) {
        return new ProfileResponse(
                profile.profileId(),
                profile.totalExperience(),
                profile.level(),
                profile.experience(),
                profile.experienceAtCurrentLevel(),
                profile.experienceAtNextLevel(),
                profile.experienceRequiredToCompleteCurrentLevel(),
                profile.wins(),
                profile.losses(),
                profile.draws(),
                profile.totalGames(),
                profile.winPercentage(),
                profile.lossPercentage(),
                profile.drawPercentage(),
                profile.winStreak(),
                profile.lossStreak(),
                profile.drawStreak(),
                profile.topics().entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> toStatisticsResponse(entry.getValue())
                        )),
                profile.games().stream()
                        .sorted(Comparator.comparing(ProfileGame::playedAt).reversed())
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

    private static GameResultResponse toResponse(ProfileGame gameResult) {
        return new GameResultResponse(
                gameResult.gameId(),
                gameResult.topicId(),
                gameResult.opponentId(),
                gameResult.opponentName(),
                gameResult.playerScore(),
                gameResult.opponentScore(),
                gameResult.result(),
                gameResult.playedAt()
        );
    }
}

