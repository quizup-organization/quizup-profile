package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.command.ProgressionCommand;
import io.github.quizup.profile.domain.event.ProgressionEvent;
import io.github.quizup.profile.domain.model.Badge;
import io.github.quizup.profile.domain.model.BadgeRules;
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
 *
 * <p>L'agrégat porte les compteurs nécessaires à l'évaluation des badges (duels et victoires
 * **entre joueurs**, séries par thème) : les règles de badge sont purement locales, sans aucune
 * lecture externe.</p>
 */
@Aggregate
public class PlayerProgressAggregate {

    @AggregateIdentifier
    private String progressId;

    private String userId;

    private final Map<String, Integer> xpByTopic = new HashMap<>();
    private final Set<String> awardedGameIds = new HashSet<>();
    private final Set<Badge> badges = new HashSet<>();
    private final Map<String, Integer> winStreakByTopic = new HashMap<>();

    private int xpTotal;
    private int level = 1;
    private String title = ProgressionRules.titleFor(1);
    private int humanDuels;
    private int humanWins;

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

        // Les compteurs (duels et victoires entre joueurs, séries, XP du sujet) sont à jour :
        // l'événement d'attribution vient d'être appliqué, donc le temps réel et le replay
        // évaluent sur le même état.
        BadgeRules.DuelFacts facts = new BadgeRules.DuelFacts(
                command.gameScore(),
                command.won(),
                command.fastAnswers(),
                humanDuels,
                humanWins,
                xpByTopic.getOrDefault(command.topicId(), 0),
                winStreakByTopic.getOrDefault(command.topicId(), 0),
                // Sujets réellement joués (XP > 0) : l'entraînement (0 XP) n'explore rien.
                (int) xpByTopic.values().stream().filter(value -> value > 0).count()
        );

        // Ordre stable (EnumSet) : le flux d'événements reste déterministe au replay.
        for (Badge badge : BadgeRules.evaluate(facts, badges)) {
            AggregateLifecycle.apply(new ProgressionEvent.BadgeEarnedEvent(
                    command.userId(),
                    badge,
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

        // Compteurs de badges : seuls les duels entre joueurs comptent (l'entraînement contre
        // bot n'alimente ni le volume, ni les victoires).
        if (!event.botGame()) {
            this.humanDuels++;
            if (event.won()) {
                this.humanWins++;
            }
        }

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
