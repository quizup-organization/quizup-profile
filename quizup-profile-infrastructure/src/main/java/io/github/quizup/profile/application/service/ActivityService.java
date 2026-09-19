package io.github.quizup.profile.application.service;

import io.github.quizup.profile.domain.model.ActivityDay;
import io.github.quizup.profile.domain.model.ActivityView;
import io.github.quizup.profile.domain.model.PlayerActivity;
import io.github.quizup.profile.domain.port.in.GetActivityUseCase;
import io.github.quizup.profile.domain.port.out.ActivityRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Activité journalière d'un joueur : série courante/record et détail des jours joués.
 */
@Service
public class ActivityService implements GetActivityUseCase {

    private final ActivityRepositoryPort activityRepositoryPort;

    public ActivityService(ActivityRepositoryPort activityRepositoryPort) {
        this.activityRepositoryPort = activityRepositoryPort;
    }

    @Override
    public ActivityView get(String userId, LocalDate from, LocalDate to) {
        PlayerActivity activity = activityRepositoryPort.findActivity(userId)
                .orElseGet(() -> PlayerActivity.empty(userId));
        List<ActivityDay> days = activityRepositoryPort.findDays(userId, from, to);

        return new ActivityView(
                userId,
                activity.currentStreak(),
                activity.longestStreak(),
                activity.lastActiveDate(),
                days.size(),
                days
        );
    }
}
