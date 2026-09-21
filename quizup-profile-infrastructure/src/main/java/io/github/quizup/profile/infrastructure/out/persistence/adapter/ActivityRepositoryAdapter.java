package io.github.quizup.profile.infrastructure.out.persistence.adapter;

import io.github.quizup.profile.domain.model.ActivityDay;
import io.github.quizup.profile.domain.model.PlayerActivity;
import io.github.quizup.profile.domain.port.out.ActivityRepositoryPort;
import io.github.quizup.profile.infrastructure.out.persistence.entity.ActivityDayEntity;
import io.github.quizup.profile.infrastructure.out.persistence.entity.ActivityDayGameEntity;
import io.github.quizup.profile.infrastructure.out.persistence.mapper.ActivityEntityMapper;
import io.github.quizup.profile.infrastructure.out.persistence.repository.ActivityDayGameJpaRepository;
import io.github.quizup.profile.infrastructure.out.persistence.repository.ActivityDayJpaRepository;
import io.github.quizup.profile.infrastructure.out.persistence.repository.ActivityJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class ActivityRepositoryAdapter implements ActivityRepositoryPort {

    private final ActivityJpaRepository activityJpaRepository;
    private final ActivityDayJpaRepository activityDayJpaRepository;
    private final ActivityDayGameJpaRepository activityDayGameJpaRepository;

    public ActivityRepositoryAdapter(ActivityJpaRepository activityJpaRepository,
                                     ActivityDayJpaRepository activityDayJpaRepository,
                                     ActivityDayGameJpaRepository activityDayGameJpaRepository) {
        this.activityJpaRepository = activityJpaRepository;
        this.activityDayJpaRepository = activityDayJpaRepository;
        this.activityDayGameJpaRepository = activityDayGameJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlayerActivity> findActivity(String userId) {
        return activityJpaRepository.findById(userId).map(ActivityEntityMapper::toDomain);
    }

    @Override
    @Transactional
    public void saveActivity(PlayerActivity activity) {
        activityJpaRepository.save(ActivityEntityMapper.toEntity(activity));
    }

    @Override
    @Transactional
    public boolean incrementDay(String userId, LocalDate date, String gameId) {
        ActivityDayGameEntity.ActivityDayGameId gameEntryId =
                new ActivityDayGameEntity.ActivityDayGameId(userId, date, gameId);
        if (activityDayGameJpaRepository.existsById(gameEntryId)) {
            return false;
        }
        activityDayGameJpaRepository.save(new ActivityDayGameEntity(userId, date, gameId));

        int gamesCount = (int) activityDayGameJpaRepository.countByUserIdAndActivityDate(userId, date);
        ActivityDayEntity.ActivityDayId id = new ActivityDayEntity.ActivityDayId(userId, date);
        ActivityDayEntity day = activityDayJpaRepository.findById(id)
                .orElseGet(() -> {
                    ActivityDayEntity created = new ActivityDayEntity();
                    created.setUserId(userId);
                    created.setActivityDate(date);
                    return created;
                });
        day.setGamesCount(gamesCount);
        activityDayJpaRepository.save(day);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityDay> findDays(String userId, LocalDate from, LocalDate to) {
        return activityDayJpaRepository
                .findByUserIdAndActivityDateBetweenOrderByActivityDateAsc(userId, from, to).stream()
                .map(ActivityEntityMapper::toDomain)
                .toList();
    }
}
