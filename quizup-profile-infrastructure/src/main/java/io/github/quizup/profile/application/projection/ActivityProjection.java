package io.github.quizup.profile.application.projection;

import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.profile.domain.model.ActivityRules;
import io.github.quizup.profile.domain.model.PlayerActivity;
import io.github.quizup.profile.domain.port.out.ActivityRepositoryPort;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * ActivityProjection — alimente l'activité journalière (série de jours joués + compteur de
 * parties par jour) à partir des fins de partie.
 *
 * <p>Consomme {@link GameEvent.GameEndedEvent} : chaque joueur humain voit son jour actif
 * crédité (fuseau {@code app.activity.zone}). La série est calculée de façon idempotente par
 * {@link ActivityRules} — rejouer le même jour n'incrémente rien.</p>
 */
@Component
public class ActivityProjection {

    private static final Logger logger = LoggerFactory.getLogger(ActivityProjection.class);

    private final ActivityRepositoryPort activityRepositoryPort;
    private final ZoneId activityZone;

    public ActivityProjection(ActivityRepositoryPort activityRepositoryPort,
                              @Value("${app.activity.zone:Europe/Paris}") String activityZone) {
        this.activityRepositoryPort = activityRepositoryPort;
        this.activityZone = ZoneId.of(activityZone);
    }

    @EventHandler
    @Transactional
    public void on(GameEvent.GameEndedEvent event) {
        LocalDate activeDate = event.endedAt().atZone(activityZone).toLocalDate();
        record(event.player1Id(), activeDate);
        record(event.player2Id(), activeDate);
    }

    private void record(String userId, LocalDate activeDate) {
        if (userId == null || QuizUpConstants.BOT_USER_ID.equals(userId)) {
            return;
        }

        PlayerActivity current = activityRepositoryPort.findActivity(userId)
                .orElseGet(() -> PlayerActivity.empty(userId));
        activityRepositoryPort.saveActivity(ActivityRules.advance(current, activeDate));
        activityRepositoryPort.incrementDay(userId, activeDate);

        logger.debug("Activité projetée: userId={}, date={}", userId, activeDate);
    }
}
