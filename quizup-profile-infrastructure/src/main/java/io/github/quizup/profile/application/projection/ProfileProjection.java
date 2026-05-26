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
        logger.debug("Projecting GameResultAddedEvent: profileId={}, gameId={}", event.profileId(), event.game().gameId());

        profileRepositoryPort.findById(event.profileId()).ifPresent(existing -> {
            ProfileGame game = event.game();

            int xpEarned = ProfileRules.computeXpEarned(game.playerScore(), game.result());

            int updatedWins = existing.wins() + (game.result() == GameResult.WIN ? 1 : 0);
            int updatedLosses = existing.losses() + (game.result() == GameResult.LOSS ? 1 : 0);
            int updatedDraws = existing.draws() + (game.result() == GameResult.DRAW ? 1 : 0);

            int winStreak = existing.winStreak();
            int lossStreak = existing.lossStreak();
            int drawStreak = existing.drawStreak();

            ProfileGame previousGame = existing.games().isEmpty()
                    ? null
                    : existing.games().get(existing.games().size() - 1);

            if (previousGame != null) {
                switch (previousGame.result()) {
                    case WIN -> winStreak = game.result() == GameResult.WIN ? winStreak + 1 : 0;
                    case LOSS -> lossStreak = game.result() == GameResult.LOSS ? lossStreak + 1 : 0;
                    case DRAW -> drawStreak = game.result() == GameResult.DRAW ? drawStreak + 1 : 0;
                }
            }

            Map<String, ProfileTopic> updatedTopics = new HashMap<>(existing.topics());
            ProfileTopic topicStatistics = updatedTopics.getOrDefault(game.topicId(), ProfileTopic.empty(game.topicId()));
            updatedTopics.put(
                    game.topicId(),
                    new ProfileTopic(
                            game.topicId(),
                            topicStatistics.totalExperience() + xpEarned,
                            topicStatistics.wins() + (game.result() == GameResult.WIN ? 1 : 0),
                            topicStatistics.losses() + (game.result() == GameResult.LOSS ? 1 : 0),
                            topicStatistics.draws() + (game.result() == GameResult.DRAW ? 1 : 0)
                    )
            );

            List<ProfileGame> updatedGames = new ArrayList<>(existing.games());
            updatedGames.add(game);

            if (updatedGames.size() > ProfileRules.MAX_RECENT_GAMES) {
                updatedGames.remove(0);
            }

            profileRepositoryPort.save(
                    existing.toBuilder()
                            .totalExperience(existing.totalExperience() + xpEarned)
                            .wins(updatedWins)
                            .losses(updatedLosses)
                            .draws(updatedDraws)
                            .winStreak(winStreak)
                            .lossStreak(lossStreak)
                            .drawStreak(drawStreak)
                            .topics(updatedTopics)
                            .games(updatedGames)
                            .updatedAt(event.recordedAt())
                            .build()
            );
        });
    }
}

