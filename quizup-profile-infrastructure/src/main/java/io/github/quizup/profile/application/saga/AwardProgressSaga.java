package io.github.quizup.profile.application.saga;

import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.profile.domain.command.ProgressionCommand;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.ProgressionRules;
import io.github.quizup.profile.domain.query.ProgressionQuery;
import io.github.quizup.profile.infrastructure.properties.AppProperties;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AwardProgressSaga — attribue l'XP et l'Elo aux joueurs à la fin d'un duel.
 *
 * <p>Consomme {@link GameEvent.GameEndedEvent} et envoie un
 * {@link ProgressionCommand.AwardXpCommand} par joueur humain. Les deux ratings courants sont
 * lus en **une seule query batch** avant l'envoi : l'agrégat de progression reste pur et calcule
 * lui-même le nouvel Elo (règles {@code EloRules}). L'idempotence (un duel = une attribution)
 * est portée par l'agrégat.</p>
 */
@Saga
@ProcessingGroup("award-progress-saga")
public class AwardProgressSaga {

    private static final Logger logger = LoggerFactory.getLogger(AwardProgressSaga.class);

    @Autowired
    private transient CommandGateway commandGateway;

    @Autowired
    private transient QueryGateway queryGateway;

    @Autowired
    private transient AppProperties appProperties;

    @StartSaga
    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.GameEndedEvent event) {
        boolean player1Won = event.winnerId() != null && event.winnerId().equals(event.player1Id());
        boolean player2Won = event.winnerId() != null && event.winnerId().equals(event.player2Id());
        boolean draw = event.winnerId() == null;
        boolean botGame = QuizUpConstants.SYSTEM_USER_ID.equals(event.player2Id())
                || QuizUpConstants.SYSTEM_USER_ID.equals(event.player1Id());
        LocalDate activeDay = event.endedAt()
                .atZone(ZoneId.of(appProperties.activity().zone()))
                .toLocalDate();

        if (botGame || event.player1Id() == null || event.player2Id() == null) {
            award(event.player1Id(), event, player1Won, draw, true,
                    event.player1CorrectAnswers(), event.player1FastAnswers(),
                    event.player1ImageCorrectAnswers(), null, null, activeDay);
            award(event.player2Id(), event, player2Won, draw, true,
                    event.player2CorrectAnswers(), event.player2FastAnswers(),
                    event.player2ImageCorrectAnswers(), null, null, activeDay);
            SagaLifecycle.end();
            return;
        }

        // Les deux ratings sont lus ensemble : une seule query, aucun accès bloquant à un service.
        queryGateway
                .query(new ProgressionQuery.GetProgressionsByIdsQuery(
                                List.of(event.player1Id(), event.player2Id())),
                        QueryResponseTypes.multipleInstancesOf(PlayerProgress.class))
                .whenComplete((progressions, error) -> {
                    try {
                        if (error != null) {
                            logger.warn("Ratings indisponibles pour gameId={} : Elo non attribué",
                                    event.gameId(), error);
                        }
                        Map<String, PlayerProgress> byId = progressions == null
                                ? Map.of()
                                : progressions.stream().collect(Collectors.toMap(
                                        PlayerProgress::userId, Function.identity(), (a, _) -> a));
                        PlayerProgress p1 = byId.get(event.player1Id());
                        PlayerProgress p2 = byId.get(event.player2Id());
                        Integer rating1 = p1 == null ? null : p1.rating();
                        Integer rating2 = p2 == null ? null : p2.rating();

                        award(event.player1Id(), event, player1Won, draw, false,
                                event.player1CorrectAnswers(), event.player1FastAnswers(),
                                event.player1ImageCorrectAnswers(),
                                event.player2Id(), rating2, activeDay);
                        award(event.player2Id(), event, player2Won, draw, false,
                                event.player2CorrectAnswers(), event.player2FastAnswers(),
                                event.player2ImageCorrectAnswers(),
                                event.player1Id(), rating1, activeDay);

                        logger.info("Progression attribuée: gameId={}, topicId={}, winner={}",
                                event.gameId(), event.topicId(), event.winnerId());
                    } finally {
                        SagaLifecycle.end();
                    }
                });
    }

    private void award(String playerId, GameEvent.GameEndedEvent event, boolean won, boolean draw,
                       boolean botGame, int correctAnswers, int fastAnswers,
                       int imageCorrectAnswers, String opponentId, Integer opponentRating,
                       LocalDate ratedDay) {
        if (playerId == null || QuizUpConstants.SYSTEM_USER_ID.equals(playerId)) {
            return;
        }

        int score = playerId.equals(event.player1Id())
                ? event.player1FinalScore()
                : event.player2FinalScore();

        // Fire-and-forget : sendAndWait bloquerait le thread du tracking processor (perf).
        commandGateway.send(new ProgressionCommand.AwardXpCommand(
                ProgressionRules.progressIdFor(playerId),
                playerId,
                event.gameId(),
                event.topicId(),
                score,
                won,
                draw,
                botGame,
                correctAnswers,
                fastAnswers,
                imageCorrectAnswers,
                opponentId,
                opponentRating,
                ratedDay
        ));
    }
}
