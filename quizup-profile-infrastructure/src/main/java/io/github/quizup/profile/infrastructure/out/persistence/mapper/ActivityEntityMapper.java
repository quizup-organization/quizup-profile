package io.github.quizup.profile.infrastructure.out.persistence.mapper;

import io.github.quizup.profile.domain.model.ActivityDay;
import io.github.quizup.profile.domain.model.PlayerActivity;
import io.github.quizup.profile.infrastructure.out.persistence.entity.ActivityDayEntity;
import io.github.quizup.profile.infrastructure.out.persistence.entity.ActivityEntity;

public final class ActivityEntityMapper {

    private ActivityEntityMapper() {
    }

    public static PlayerActivity toDomain(ActivityEntity entity) {
        return PlayerActivity.builder()
                .userId(entity.getUserId())
                .currentStreak(entity.getCurrentStreak())
                .longestStreak(entity.getLongestStreak())
                .lastActiveDate(entity.getLastActiveDate())
                .build();
    }

    public static ActivityEntity toEntity(PlayerActivity activity) {
        ActivityEntity entity = new ActivityEntity();
        entity.setUserId(activity.userId());
        entity.setCurrentStreak(activity.currentStreak());
        entity.setLongestStreak(activity.longestStreak());
        entity.setLastActiveDate(activity.lastActiveDate());
        return entity;
    }

    public static ActivityDay toDomain(ActivityDayEntity entity) {
        return new ActivityDay(entity.getActivityDate(), entity.getGamesCount());
    }
}
