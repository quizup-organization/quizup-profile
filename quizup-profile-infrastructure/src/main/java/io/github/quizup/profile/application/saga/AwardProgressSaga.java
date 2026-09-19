package io.github.quizup.profile.application.saga;

import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.profile.domain.command.ProgressionCommand;
import io.github.quizup.profile.domain.model.ProgressionRules;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * AwardProgressSaga — attribue l'XP aux joueurs à la fin d'un duel.
 *
 * <p>Consomme {@link GameEvent.GameEndedEvent} (service {@code quizup-game}) et
 * envoie un {@link ProgressionCommand.AwardXpCommand} par joueur humain.
 * L'idempotence (un duel = une attribution) est portée par l'agrégat.</p>
 */
@Saga
public class AwardProgressSaga {

    private static final Logger logger = LoggerFactory.getLogger(AwardProgressSaga.class);

    @Autowired
    private transient CommandGateway commandGateway;

    @StartSaga
    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.GameEndedEvent event) {
        boolean player1Won = event.winnerId() != null && event.winnerId().equals(event.player1Id());
        boolean player2Won = event.winnerId() != null && event.winnerId().equals(event.player2Id());

        award(event.player1Id(), event, player1Won, event.player1CorrectAnswers(), event.player1FastAnswers());
        award(event.player2Id(), event, player2Won, event.player2CorrectAnswers(), event.player2FastAnswers());

        logger.info("Progression attribuée: gameId={}, topicId={}, winner={}",
                event.gameId(), event.topicId(), event.winnerId());

        SagaLifecycle.end();
    }

    private void award(String playerId, GameEvent.GameEndedEvent event, boolean won,
                       int correctAnswers, int fastAnswers) {
        if (playerId == null || QuizUpConstants.BOT_USER_ID.equals(playerId)) {
            return;
        }

        int score = playerId.equals(event.player1Id())
                ? event.player1FinalScore()
                : event.player2FinalScore();

        commandGateway.send(new ProgressionCommand.AwardXpCommand(
                ProgressionRules.progressIdFor(playerId),
                playerId,
                event.gameId(),
                event.topicId(),
                score,
                won,
                correctAnswers,
                fastAnswers
        ));
    }
}
