package io.github.quizup.profile.application.projection;

import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.model.*;
import io.github.quizup.profile.domain.port.out.ProfileRepositoryPort;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
        logger.debug("Projecting GameResultAddedEvent: profileId={}, gameId={}", event.profileId(), event.game().gameId());

        profileRepositoryPort.findById(event.profileId()).ifPresent(existing -> {
            ProfileGame game = event.game();

            int xpEarned = ProfileRules.computeXpEarned(game.playerScore(), game.result());

            int updatedWins = existing.wins() + (game.result() == GameResult.WIN ? 1 : 0);
            int updatedLosses = existing.losses() + (game.result() == GameResult.LOSS ? 1 : 0);
            int updatedDraws = existing.draws() + (game.result() == GameResult.DRAW ? 1 : 0);

            ProfileGame previousGame = existing.games().isEmpty()
                    ? null
                    : existing.games().getLast();

            Streak profileStreak = ProfileRules.computeStreak(
                    ProfileStreak.of(existing.winStreak(), existing.lossStreak(), existing.drawStreak()),
                    game,
                    previousGame
            );

            Map<String, ProfileTopic> updatedTopics = new HashMap<>(existing.topics());
            ProfileTopic existingTopic = updatedTopics.getOrDefault(game.topicId(), ProfileTopic.empty(game.topicId()));
            updatedTopics.put(game.topicId(), updateTopic(existingTopic, game, xpEarned));

            List<ProfileGame> updatedGames = new ArrayList<>(existing.games());
            updatedGames.add(game);

            if (updatedGames.size() > ProfileRules.MAX_RECENT_GAMES) {
                updatedGames.removeFirst();
            }

            profileRepositoryPort.save(
                    existing.toBuilder()
                            .totalExperience(existing.totalExperience() + xpEarned)
                            .wins(updatedWins)
                            .losses(updatedLosses)
                            .draws(updatedDraws)
                            .winStreak(profileStreak.winStreak())
                            .lossStreak(profileStreak.lossStreak())
                            .drawStreak(profileStreak.drawStreak())
                            .topics(updatedTopics)
                            .games(updatedGames)
                            .updatedAt(event.recordedAt())
                            .build()
            );
        });
    }

    private ProfileTopic updateTopic(ProfileTopic topic, ProfileGame game, int xpEarned) {
        // Mirror du comportement de ProfileTopicAggregate: streak basé uniquement sur le résultat courant.
        Streak topicStreak = ProfileRules.computeStreak(
                ProfileStreak.of(topic.winStreak(), topic.lossStreak(), topic.drawStreak()),
                game.result(),
                game.result()
        );

        return new ProfileTopic(
                topic.topicId(),
                topic.totalExperience() + xpEarned,
                topic.wins() + (game.result() == GameResult.WIN ? 1 : 0),
                topic.losses() + (game.result() == GameResult.LOSS ? 1 : 0),
                topic.draws() + (game.result() == GameResult.DRAW ? 1 : 0),
                Collections.emptyList(),
                topicStreak.winStreak(),
                topicStreak.drawStreak(),
                topicStreak.lossStreak()
        );
    }
}

