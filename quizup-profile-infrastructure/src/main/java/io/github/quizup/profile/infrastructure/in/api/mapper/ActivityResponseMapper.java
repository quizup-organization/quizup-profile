package io.github.quizup.profile.infrastructure.in.api.mapper;

import io.github.quizup.profile.domain.model.ActivityView;
import io.github.quizup.profile.infrastructure.in.api.response.ActivityDayResponse;
import io.github.quizup.profile.infrastructure.in.api.response.ActivityResponse;

import java.util.List;

public final class ActivityResponseMapper {

    private ActivityResponseMapper() {
    }

    public static ActivityResponse toResponse(ActivityView view) {
        List<ActivityDayResponse> days = view.days().stream()
                .map(day -> new ActivityDayResponse(day.date(), day.games()))
                .toList();

        return new ActivityResponse(
                view.userId(),
                view.currentStreak(),
                view.longestStreak(),
                view.lastActiveDate(),
                view.totalActiveDays(),
                days
        );
    }
}
