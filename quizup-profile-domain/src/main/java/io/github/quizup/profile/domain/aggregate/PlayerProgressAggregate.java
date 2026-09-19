package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.command.ProgressionCommand;
import io.github.quizup.profile.domain.event.ProgressionEvent;
import io.github.quizup.profile.domain.model.Badge;
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
 * niveau, titre, badges).
 *
 * <p>Identifiant d'agrégat namespacé ({@code progress:<userId>}). Créé à la volée
 * au premier {@code AwardXpCommand} ({@link AggregateCreationPolicy#CREATE_IF_MISSING}).
 * L'attribution est idempotente : un même {@code gameId} n'est crédité qu'une fois.</p>
 */
@Aggregate
public class PlayerProgressAggregate {

    private static final int LIGHTNING_FAST_ANSWERS = 5;
    private static final int STREAK_MASTER_WINS = 10;

    @AggregateIdentifier
    private String progressId;

    private String userId;

    private final Map<String, Integer> xpByTopic = new HashMap<>();
    private final Set<String> awardedGameIds = new HashSet<>();
    private final Set<Badge> badges = new HashSet<>();
    private final Map<String, Integer> winStreakByTopic = new HashMap<>();

    private int xpTotal;
    private int fastAnswersTotal;
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

        int xp = ProgressionRules.xpFor(command.gameScore(), command.won());
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
                command.correctAnswers(),
                command.fastAnswers(),
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

        if (command.won() && !badges.contains(Badge.FIRST_WIN)) {
            AggregateLifecycle.apply(new ProgressionEvent.BadgeEarnedEvent(
                    command.userId(),
                    Badge.FIRST_WIN,
                    now
            ));
        }

        if (command.gameScore() >= ProgressionRules.PERFECT_SCORE && !badges.contains(Badge.PERFECT)) {
            AggregateLifecycle.apply(new ProgressionEvent.BadgeEarnedEvent(
                    command.userId(),
                    Badge.PERFECT,
                    now
            ));
        }

        if (fastAnswersTotal >= LIGHTNING_FAST_ANSWERS && !badges.contains(Badge.LIGHTNING)) {
            AggregateLifecycle.apply(new ProgressionEvent.BadgeEarnedEvent(
                    command.userId(),
                    Badge.LIGHTNING,
                    now
            ));
        }

        if (winStreakByTopic.getOrDefault(command.topicId(), 0) >= STREAK_MASTER_WINS
                && !badges.contains(Badge.STREAK_MASTER)) {
            AggregateLifecycle.apply(new ProgressionEvent.BadgeEarnedEvent(
                    command.userId(),
                    Badge.STREAK_MASTER,
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
        this.fastAnswersTotal += event.fastAnswers();
        this.level = ProgressionRules.levelFor(this.xpTotal);
        this.title = ProgressionRules.titleFor(this.level);

        if (event.won()) {
            this.winStreakByTopic.merge(event.topicId(), 1, Integer::sum);
        } else {
            this.winStreakByTopic.put(event.topicId(), 0);
        }
    }

    @EventSourcingHandler
    public void on(ProgressionEvent.LevelReachedEvent event) {
        this.level = event.level();
        this.title = event.title();
    }

    @EventSourcingHandler
    public void on(ProgressionEvent.BadgeEarnedEvent event) {
        this.badges.add(event.badge());
    }
}
