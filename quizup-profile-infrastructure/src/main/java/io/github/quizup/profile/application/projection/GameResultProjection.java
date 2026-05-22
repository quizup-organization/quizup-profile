package io.github.quizup.profile.application.projection;

import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.model.GameResult;
import io.github.quizup.profile.domain.model.GameResultType;
import io.github.quizup.profile.domain.port.out.TopicPort;
import io.github.quizup.profile.domain.port.out.UserPort;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GameResultProjection {

    private static final Logger logger = LoggerFactory.getLogger(GameResultProjection.class);

    private final CommandGateway commandGateway;
    private final UserPort userPort;
    private final TopicPort topicPort;

    public GameResultProjection(CommandGateway commandGateway,
                                UserPort userPort,
                                TopicPort topicPort) {
        this.commandGateway = commandGateway;
        this.userPort = userPort;
        this.topicPort = topicPort;
    }

    @EventHandler
    public void on(GameEvent.GameEndedEvent event) {
        String topicName = resolveTopicName(event.topicId());
        String player1Name = resolveUserName(event.player1Id());
        String player2Name = resolveUserName(event.player2Id());

        GameResult player1Result = new GameResult(
                event.gameId(),
                event.topicId(),
                topicName,
                event.player2Id(),
                player2Name,
                event.player1FinalScore(),
                event.player2FinalScore(),
                resolveResultType(event.winnerId(), event.player1Id()),
                event.endedAt()
        );

        GameResult player2Result = new GameResult(
                event.gameId(),
                event.topicId(),
                topicName,
                event.player1Id(),
                player1Name,
                event.player2FinalScore(),
                event.player1FinalScore(),
                resolveResultType(event.winnerId(), event.player2Id()),
                event.endedAt()
        );

        commandGateway.send(new ProfileCommand.AddGameResultCommand(event.player1Id(), player1Result))
                .exceptionally(error -> {
                    logger.warn("Unable to project game result for profile {}: {}", event.player1Id(), error.getMessage());
                    return null;
                });

        commandGateway.send(new ProfileCommand.AddGameResultCommand(event.player2Id(), player2Result))
                .exceptionally(error -> {
                    logger.warn("Unable to project game result for profile {}: {}", event.player2Id(), error.getMessage());
                    return null;
                });
    }

    private GameResultType resolveResultType(String winnerId, String playerId) {
        if (winnerId == null) {
            return GameResultType.DRAW;
        }
        return winnerId.equals(playerId) ? GameResultType.WIN : GameResultType.LOSS;
    }

    private String resolveUserName(String userId) {
        return userPort.findById(userId).map(profileUser -> profileUser.name()).orElse(userId);
    }

    private String resolveTopicName(String topicId) {
        return topicPort.findById(topicId).map(profileTopic -> profileTopic.name()).orElse(topicId);
    }
}

