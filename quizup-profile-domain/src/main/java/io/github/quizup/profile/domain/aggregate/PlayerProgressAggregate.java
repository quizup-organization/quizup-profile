package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.command.ProgressionCommand;
import io.github.quizup.profile.domain.event.ProgressionEvent;
import io.github.quizup.profile.domain.model.EloRules;
import io.github.quizup.profile.domain.model.MasteryGrade;
import io.github.quizup.profile.domain.model.ProgressionRules;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * PlayerProgressAggregate — progression d'un joueur (XP totale + XP par thème,
 * niveau, titre). Les **badges** vivent dans `BadgeAggregate`.
 *
 * <p>Identifiant d'agrégat namespacé ({@code progress:<userId>}). Créé à la volée
 * au premier {@code AwardXpCommand} ({@link AggregateCreationPolicy#CREATE_IF_MISSING}).
 * L'attribution est idempotente : un même {@code gameId} n'est crédité qu'une fois.</p>
 *
 */
@Aggregate
public class PlayerProgressAggregate {

    @AggregateIdentifier
    private String progressId;

    private String userId;

    private final Map<String, Integer> xpByTopic = new HashMap<>();
    private final Set<String> awardedGameIds = new HashSet<>();
    /** Nombre de duels classés contre un adversaire pour un jour local : plafond anti-farm. */
    private final Map<String, Integer> ratedOpponentDayCounts = new HashMap<>();

    private int xpTotal;
    private int level = 1;
    private String title = ProgressionRules.titleFor(1);
    private int rating = EloRules.INITIAL_RATING;
    private int peakRating = EloRules.INITIAL_RATING;
    private int ratedGames;
    private boolean provisional = true;

    protected PlayerProgressAggregate() {
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(ProgressionCommand.AwardXpCommand command) {
        if (this.progressId == null) {
            this.progressId = command.progressId();
        }
        if (this.userId == null) {
            this.userId = command.userId();
        }

        if (awardedGameIds.contains(command.gameId())) {
            return;
        }

        int xp = ProgressionRules.xpFor(command.gameScore(), command.won(), command.botGame());
        int newTotal = xpTotal + xp;
        int previousLevel = level;
        Instant now = Instant.now();

        int newRating = rating;
        int newPeakRating = peakRating;
        int newRatedGames = ratedGames;
        int ratingDelta = 0;
        String ratedOpponentId = null;
        java.time.LocalDate ratedDay = null;

        if (!command.botGame()
                && command.opponentId() != null
                && command.opponentRating() != null
                && command.ratedDay() != null) {
            String opponentDayKey = command.ratedDay() + "|" + command.opponentId();
            int playedToday = ratedOpponentDayCounts.getOrDefault(opponentDayKey, 0);
            if (playedToday < EloRules.DAILY_OPPONENT_CAP) {
                double score = command.draw() ? 0.5 : command.won() ? 1.0 : 0.0;
                newRating = EloRules.newRating(rating, command.opponentRating(), score, ratedGames);
                newPeakRating = Math.max(peakRating, newRating);
                newRatedGames = ratedGames + 1;
                ratingDelta = newRating - rating;
                ratedOpponentId = command.opponentId();
                ratedDay = command.ratedDay();
            }
        }

        AggregateLifecycle.apply(new ProgressionEvent.XpAwardedEvent(
                command.userId(),
                command.gameId(),
                command.topicId(),
                xp,
                command.gameScore(),
                command.won(),
                command.draw(),
                command.botGame(),
                command.correctAnswers(),
                command.fastAnswers(),
                command.imageCorrectAnswers(),
                now,
                newRating,
                newPeakRating,
                newRatedGames,
                EloRules.isProvisional(newRatedGames),
                ratingDelta,
                ratedOpponentId,
                ratedDay
        ));

        // Titre thématique : le grade de maîtrise du sujet franchi est annoncé (célébration).
        int topicXpAfter = xpByTopic.getOrDefault(command.topicId(), 0);
        MasteryGrade gradeBefore = MasteryGrade.gradeFor(topicXpAfter - xp);
        MasteryGrade gradeAfter = MasteryGrade.gradeFor(topicXpAfter);
        if (gradeAfter.ordinal() > gradeBefore.ordinal()) {
            AggregateLifecycle.apply(new ProgressionEvent.MasteryGradeReachedEvent(
                    command.userId(),
                    command.topicId(),
                    gradeAfter,
                    now
            ));
        }

        int newLevel = ProgressionRules.levelFor(newTotal);

        if (newLevel > previousLevel) {
            AggregateLifecycle.apply(new ProgressionEvent.LevelReachedEvent(
                    command.userId(),
                    newLevel,
                    ProgressionRules.titleFor(newLevel),
                    now
            ));
        }

    }

    @EventSourcingHandler
    public void on(ProgressionEvent.XpAwardedEvent event) {
        this.progressId = ProgressionRules.progressIdFor(event.userId());
        this.userId = event.userId();
        this.xpByTopic.merge(event.topicId(), event.xp(), Integer::sum);
        this.awardedGameIds.add(event.gameId());
        this.xpTotal += event.xp();
        this.level = ProgressionRules.levelFor(this.xpTotal);
        this.title = ProgressionRules.titleFor(this.level);
        this.rating = event.rating();
        this.peakRating = event.peakRating();
        this.ratedGames = event.ratedGames();
        this.provisional = event.provisional();
        if (event.ratedOpponentId() != null && event.ratedDay() != null) {
            this.ratedOpponentDayCounts.merge(
                    event.ratedDay() + "|" + event.ratedOpponentId(), 1, Integer::sum);
        }
    }

    @EventSourcingHandler
    public void on(ProgressionEvent.LevelReachedEvent event) {
        this.level = event.level();
        this.title = event.title();
    }

}
