package io.github.quizup.profile.application.projection;

import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.event.ProfileTopicEvent;
import io.github.quizup.profile.domain.model.*;
import io.github.quizup.profile.domain.port.out.ProfileRepositoryPort;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProfileProjection {

    private static final Logger logger = LoggerFactory.getLogger(ProfileProjection.class);

    private final ProfileRepositoryPort profileRepositoryPort;

    public ProfileProjection(ProfileRepositoryPort profileRepositoryPort) {
        this.profileRepositoryPort = profileRepositoryPort;
    }

    // ── Profile events ──────────────────────────────────────────────────────

    @EventHandler
    @Transactional
    public void on(ProfileEvent.ProfileCreatedEvent event) {
        logger.debug("Projecting ProfileCreatedEvent: profileId={}", event.profileId());
        profileRepositoryPort.save(Profile.empty(event.profileId(), event.createdAt()));
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.ExperienceIncreasedEvent event) {
        logger.debug("Projecting ExperienceIncreasedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .totalExperience(profile.totalExperience() + event.experienceEarned())
                        .updatedAt(event.earnedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.LevelIncreasedEvent event) {
        logger.debug("Projecting LevelIncreasedEvent: profileId={}, level={}", event.profileId(), event.level());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .level(event.level())
                        .updatedAt(event.increasedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.WinsIncreasedEvent event) {
        logger.debug("Projecting WinsIncreasedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .wins(event.wins())
                        .updatedAt(event.increasedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.LossesIncreasedEvent event) {
        logger.debug("Projecting LossesIncreasedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .losses(event.losses())
                        .updatedAt(event.increasedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.DrawsIncreasedEvent event) {
        logger.debug("Projecting DrawsIncreasedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .draws(event.draws())
                        .updatedAt(event.increasedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.WinStreakIncreasedEvent event) {
        logger.debug("Projecting WinStreakIncreasedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .winStreak(event.winStreak())
                        .lossStreak(0)
                        .drawStreak(0)
                        .updatedAt(event.increasedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.WinStreakEndedEvent event) {
        logger.debug("Projecting WinStreakEndedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .winStreak(0)
                        .updatedAt(event.endedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.LossStreakIncreasedEvent event) {
        logger.debug("Projecting LossStreakIncreasedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .lossStreak(event.lossStreak())
                        .winStreak(0)
                        .drawStreak(0)
                        .updatedAt(event.increasedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.LossStreakEndedEvent event) {
        logger.debug("Projecting LossStreakEndedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .lossStreak(0)
                        .updatedAt(event.endedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.DrawStreakIncreasedEvent event) {
        logger.debug("Projecting DrawStreakIncreasedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .drawStreak(event.drawStreak())
                        .winStreak(0)
                        .lossStreak(0)
                        .updatedAt(event.increasedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.DrawStreakEndedEvent event) {
        logger.debug("Projecting DrawStreakEndedEvent: profileId={}", event.profileId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile ->
                profileRepositoryPort.save(profile.toBuilder()
                        .drawStreak(0)
                        .updatedAt(event.endedAt())
                        .build())
        );
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.GamePlayedEvent event) {
        logger.debug("Projecting GamePlayedEvent: profileId={}, gameId={}", event.profileId(), event.game().gameId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile -> {
            List<ProfileGame> updatedGames = new ArrayList<>(profile.games());
            updatedGames.add(event.game());
            if (updatedGames.size() > ProfileRules.MAX_RECENT_GAMES) {
                updatedGames.removeFirst();
            }
            profileRepositoryPort.save(profile.toBuilder()
                    .games(updatedGames)
                    .updatedAt(event.playedAt())
                    .build());
        });
    }

    // ── Topic events ────────────────────────────────────────────────────────

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.ProfileTopicCreatedEvent event) {
        logger.debug("Projecting ProfileTopicCreatedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        profileRepositoryPort.findById(event.profileId()).ifPresent(profile -> {
            Map<String, ProfileTopic> updatedTopics = new HashMap<>(profile.topics());
            updatedTopics.put(event.topicId(), ProfileTopic.empty(event.topicId(), event.createdAt()));
            profileRepositoryPort.save(profile.toBuilder().topics(updatedTopics).build());
        });
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicExperienceIncreasedEvent event) {
        logger.debug("Projecting TopicExperienceIncreasedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .totalExperience(topic.totalExperience() + event.experienceEarned())
                .updatedAt(event.earnedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicLevelIncreasedEvent event) {
        logger.debug("Projecting TopicLevelIncreasedEvent: profileId={}, topicId={}, level={}", event.profileId(), event.topicId(), event.level());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .level(event.level())
                .updatedAt(event.increasedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicWinsIncreasedEvent event) {
        logger.debug("Projecting TopicWinsIncreasedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .wins(event.wins())
                .updatedAt(event.increasedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicLossesIncreasedEvent event) {
        logger.debug("Projecting TopicLossesIncreasedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .losses(event.losses())
                .updatedAt(event.increasedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicDrawsIncreasedEvent event) {
        logger.debug("Projecting TopicDrawsIncreasedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .draws(event.draws())
                .updatedAt(event.increasedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicWinStreakIncreasedEvent event) {
        logger.debug("Projecting TopicWinStreakIncreasedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .winStreak(event.winStreak())
                .lossStreak(0)
                .drawStreak(0)
                .updatedAt(event.increasedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicWinStreakEndedEvent event) {
        logger.debug("Projecting TopicWinStreakEndedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .winStreak(0)
                .updatedAt(event.endedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicLossStreakIncreasedEvent event) {
        logger.debug("Projecting TopicLossStreakIncreasedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .lossStreak(event.lossStreak())
                .winStreak(0)
                .drawStreak(0)
                .updatedAt(event.increasedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicLossStreakEndedEvent event) {
        logger.debug("Projecting TopicLossStreakEndedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .lossStreak(0)
                .updatedAt(event.endedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicDrawStreakIncreasedEvent event) {
        logger.debug("Projecting TopicDrawStreakIncreasedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .drawStreak(event.drawStreak())
                .winStreak(0)
                .lossStreak(0)
                .updatedAt(event.increasedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicDrawStreakEndedEvent event) {
        logger.debug("Projecting TopicDrawStreakEndedEvent: profileId={}, topicId={}", event.profileId(), event.topicId());
        updateTopic(event.profileId(), event.topicId(), topic -> topic.toBuilder()
                .drawStreak(0)
                .updatedAt(event.endedAt())
                .build());
    }

    @EventHandler
    @Transactional
    public void on(ProfileTopicEvent.TopicGamePlayedEvent event) {
        logger.debug("Projecting TopicGamePlayedEvent: profileId={}, topicId={}, gameId={}", event.profileId(), event.topicId(), event.game().gameId());
        updateTopic(event.profileId(), event.topicId(), topic -> {
            List<ProfileGame> updatedGames = new ArrayList<>(topic.games());
            updatedGames.add(event.game());
            if (updatedGames.size() > ProfileRules.MAX_RECENT_GAMES) {
                updatedGames.removeFirst();
            }
            return topic.toBuilder()
                    .games(updatedGames)
                    .updatedAt(event.playedAt())
                    .build();
        });
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private void updateTopic(String profileId, String topicId,
                             java.util.function.UnaryOperator<ProfileTopic> updater) {
        profileRepositoryPort.findById(profileId).ifPresent(profile -> {
            ProfileTopic existing = profile.topics().getOrDefault(topicId, ProfileTopic.empty(topicId, java.time.Instant.now()));
            Map<String, ProfileTopic> updatedTopics = new HashMap<>(profile.topics());
            updatedTopics.put(topicId, updater.apply(existing));
            profileRepositoryPort.save(profile.toBuilder().topics(updatedTopics).build());
        });
    }
}
