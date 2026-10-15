package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.command.ProgressionCommand;
import io.github.quizup.profile.domain.event.ProgressionEvent;
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

    private int xpTotal;
    private int level = 1;
    private String title = ProgressionRules.titleFor(1);

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
                now
        ));

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
    }

    @EventSourcingHandler
    public void on(ProgressionEvent.LevelReachedEvent event) {
        this.level = event.level();
        this.title = event.title();
    }

}
