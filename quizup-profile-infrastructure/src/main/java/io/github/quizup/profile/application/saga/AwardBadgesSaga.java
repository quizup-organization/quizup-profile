package io.github.quizup.profile.application.saga;

import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.profile.domain.command.ProgressionCommand;
import io.github.quizup.profile.domain.event.ProgressionEvent;
import io.github.quizup.profile.domain.model.ProgressionRules;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

/**
 * AwardBadgesSaga — déclenche l'évaluation des **badges** d'un joueur à la fin d'un duel.
 *
 * <p>Consomme {@link ProgressionEvent.XpAwardedEvent} (émis par la progression, il porte tous les
 * faits du duel : sujet, XP, score, victoire, entraînement, réponses rapides) et envoie un
 * {@link ProgressionCommand.EvaluateBadgesCommand} à l'agrégat dédié {@code badges:<userId>}.
 * Saga **sans état** : elle se termine aussitôt (l'idempotence est portée par l'agrégat, par
 * {@code gameId}).</p>
 */
@Saga
@ProcessingGroup("award-badges-saga")
public class AwardBadgesSaga {

    private static final Logger logger = LoggerFactory.getLogger(AwardBadgesSaga.class);

    @Autowired
    private transient CommandGateway commandGateway;

    @StartSaga
    @SagaEventHandler(associationProperty = "userId")
    public void on(ProgressionEvent.XpAwardedEvent event) {
        if (event.userId() == null
                || Objects.equals(QuizUpConstants.SYSTEM_USER_ID, event.userId())) {
            SagaLifecycle.end();
            return;
        }

        commandGateway.send(new ProgressionCommand.EvaluateBadgesCommand(
                ProgressionRules.badgeIdFor(event.userId()),
                event.userId(),
                event.gameId(),
                event.topicId(),
                event.xp(),
                event.gameScore(),
                event.won(),
                event.botGame(),
                event.fastAnswers()
        ));
        logger.debug("Badges évalués: userId={}, gameId={}", event.userId(), event.gameId());
        SagaLifecycle.end();
    }
}
