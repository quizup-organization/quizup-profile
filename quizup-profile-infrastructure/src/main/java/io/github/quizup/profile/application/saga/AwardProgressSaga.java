package io.github.quizup.profile.application.saga;

import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.profile.domain.command.ProgressionCommand;
import io.github.quizup.profile.domain.model.ProgressionRules;
import io.github.quizup.profile.infrastructure.properties.AppProperties;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * AwardProgressHandler — attribue l'XP et l'Elo aux joueurs à la fin d'un duel.
 *
 * <p>Handler d'événement sans état (pas de saga) : consomme {@link GameEvent.GameEndedEvent} et envoie un
 * {@link ProgressionCommand.AwardXpCommand} par joueur humain. Les deux ratings courants sont
 * lus dans la projection locale avant l'envoi : l'agrégat de progression reste pur et calcule
 * lui-même le nouvel Elo (règles {@code EloRules}). L'idempotence (un duel = une attribution)
 * est portée par l'agrégat.</p>
 */
@Component
@ProcessingGroup("award-progress-saga")
public class AwardProgressSaga {

    private static final Logger logger = LoggerFactory.getLogger(AwardProgressSaga.class);

    @Autowired
    private transient CommandGateway commandGateway;

    @Autowired
    private transient AppProperties appProperties;

    @Autowired
    private transient io.github.quizup.profile.domain.port.out.ProgressionRepositoryPort progressionRepository;

    @EventHandler
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
            return;
        }

        // Les ratings sont lus directement dans la projection locale (même base) : le handler
        // reste synchrone, sans callback hors unité de travail ni perte d'événement au redémarrage.
        var p1 = progressionRepository.findById(event.player1Id()).orElse(null);
        var p2 = progressionRepository.findById(event.player2Id()).orElse(null);
        Integer rating1 = p1 == null
                ? io.github.quizup.profile.domain.model.EloRules.INITIAL_RATING
                : p1.rating();
        Integer rating2 = p2 == null
                ? io.github.quizup.profile.domain.model.EloRules.INITIAL_RATING
                : p2.rating();

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
