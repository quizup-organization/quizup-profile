package io.github.quizup.profile.application.projection;

import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.model.*;
import io.github.quizup.profile.domain.port.out.ProfileRepositoryPort;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
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

    @EventHandler
    @Transactional
    public void on(ProfileEvent.ProfileCreatedEvent event) {
        logger.debug("Projecting ProfileCreatedEvent: profileId={}", event.profileId());
        profileRepositoryPort.save(Profile.empty(event.profileId(), event.createdAt()));
    }

    @EventHandler
    @Transactional
    public void on(ProfileEvent.GameResultAddedEvent event) {
        logger.debug("Projecting GameResultRecordedEvent: profileId={}, gameId={}", event.profileId(), event.gameId());

        profileRepositoryPort.findById(event.profileId()).ifPresent(existing -> {
            Map<String, TopicStatistics> updatedTopics = new HashMap<>(existing.topicStatistics());
            updatedTopics.put(event.topicId(), new TopicStatistics(
                    event.topicId(),
                    event.newTopicTotalExperience(),
                    event.newTopicWins(),
                    event.newTopicLosses(),
                    event.newTopicDraws(),
                    event.newTopicWinStreak()
            ));

            Map<BadgeType, Badge> updatedBadges = new EnumMap<>(BadgeType.class);
            updatedBadges.putAll(existing.badges());
            event.newBadges().forEach(badgeType -> updatedBadges.putIfAbsent(badgeType, new Badge(badgeType, event.recordedAt())));

            List<ProfileGame> updatedRecent = buildUpdatedRecentGames(existing.recentGameResults(), event);

            profileRepositoryPort.save(existing.toBuilder()
                    .globalStatistics(new GlobalStatistics(
                            event.newGlobalTotalExperience(),
                            event.newGlobalWins(),
                            event.newGlobalLosses(),
                            event.newGlobalDraws()
                    ))
                    .winStreak(event.newWinStreak())
                    .lossStreak(event.newLossStreak())
                    .drawStreak(event.newDrawStreak())
                    .topicStatistics(updatedTopics)
                    .badges(updatedBadges)
                    .recentGameResults(updatedRecent)
                    .updatedAt(event.recordedAt())
                    .build());
        });
    }

    private List<ProfileGame> buildUpdatedRecentGames(List<ProfileGame> existing, ProfileEvent.GameResultAddedEvent event) {
        ProfileGame newEntry = ProfileGame.builder()
                .gameId(event.gameId())
                .topicId(event.topicId())
                .opponentId(event.opponentId())
                .playerScore(event.playerScore())
                .opponentScore(event.opponentScore())
                .result(event.result())
                .playedAt(event.recordedAt())
                .build();

        List<ProfileGame> result = new ArrayList<>();
        result.add(newEntry);
        result.addAll(existing);
        if (result.size() > ProfileRules.MAX_RECENT_GAMES) {
            return result.subList(0, ProfileRules.MAX_RECENT_GAMES);
        }
        return result;
    }
}

