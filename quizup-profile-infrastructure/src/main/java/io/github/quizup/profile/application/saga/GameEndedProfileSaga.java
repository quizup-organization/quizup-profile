package io.github.quizup.profile.application.saga;

import io.github.quizup.common.domain.constant.QuizUpConstants;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.model.GameResult;
import lombok.Getter;
import lombok.Setter;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Saga
public class GameEndedProfileSaga {

    private static final Logger logger = LoggerFactory.getLogger(GameEndedProfileSaga.class);

    @Autowired
    private transient CommandGateway commandGateway;

    @Getter
    @Setter
    private String gameId;

    @StartSaga
    @EndSaga
    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.GameEndedEvent event) {
        this.gameId = event.gameId();
        logger.info("GameEndedProfileSaga processing game results: gameId={}", event.gameId());

        commandGateway.send(new ProfileCommand.AddGameResultCommand(
                event.player1Id(),
                event.gameId(),
                event.topicId(),
                event.player2Id(),
                event.player2Name(),
                event.player1FinalScore(),
                event.player2FinalScore(),
                determineResult(event.player1Id(), event.winnerId())
        ));

        if (!QuizUpConstants.BOT_USER_ID.equals(event.player2Id())) {
            commandGateway.send(
                    new ProfileCommand.AddGameResultCommand(
                            event.player2Id(),
                            event.gameId(),
                            event.topicId(),
                            event.player1Id(),
                            event.player1Name(),
                            event.player2FinalScore(),
                            event.player1FinalScore(),
                            determineResult(event.player2Id(), event.winnerId())
                    )
            );
        }

        SagaLifecycle.end();
    }

    private GameResult determineResult(String playerId, String winnerId) {
        if (winnerId == null) {
            return GameResult.DRAW;
        }
        return winnerId.equals(playerId) ? GameResult.WIN : GameResult.LOSS;
    }
}

