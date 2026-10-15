package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.command.ProgressionCommand;
import io.github.quizup.profile.domain.event.ProgressionEvent;
import io.github.quizup.profile.domain.model.Badge;
import io.github.quizup.profile.domain.model.ActivityRules;
import io.github.quizup.profile.domain.model.BadgeRules;
import io.github.quizup.profile.domain.model.PlayerActivity;
import io.github.quizup.profile.domain.model.ProgressionRules;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * BadgeAggregate — **succès** (badges) d'un joueur, séparé de la progression (XP, niveau,
 * titres). Identifiant {@code badges:<userId>} (namespacé, cf. {@code ProgressionRules}).
 *
 * <p>Il porte **tous** les compteurs nécessaires à l'évaluation des {@link Badge} : duels et
 * victoires **entre joueurs**, XP par sujet, séries par sujet — plus les badges déjà acquis et
 * les duels déjà évalués (idempotence). L'entraînement contre bot n'alimente aucun compteur de
 * volume/maîtrise, mais les faits d'exploit du duel restent évalués (score, réponses rapides).</p>
 *
 * <p>Créé à la volée au premier {@code EvaluateBadgesCommand}
 * ({@link AggregateCreationPolicy#CREATE_IF_MISSING}) ; un même {@code gameId} n'est évalué
 * qu'une fois. Chaque badge nouvellement acquis émet un {@link ProgressionEvent.BadgeEarnedEvent}
 * (même read model que la progression : la collection reste exposée telle quelle au BFF).</p>
 */
@Aggregate
public class BadgeAggregate {

    @AggregateIdentifier
    private String badgeId;

    private String userId;

    private final Set<String> evaluatedGameIds = new HashSet<>();
    private final Map<String, Integer> xpByTopic = new HashMap<>();
    private final Map<String, Integer> winStreakByTopic = new HashMap<>();
    private final Set<Badge> badges = new HashSet<>();

    /** Assiduité (jours joués consécutifs) — même règle pure que la projection d'activité. */
    private PlayerActivity activity = PlayerActivity.empty(null);

    private int humanDuels;
    private int humanWins;
    private int followersGained;
    private int challengesSent;
    private int perfectDuels;
    private int fastAnswersTotal;

    protected BadgeAggregate() {
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(ProgressionCommand.EvaluateBadgesCommand command) {
        if (this.badgeId == null) {
            this.badgeId = command.badgeId();
        }
        if (this.userId == null) {
            this.userId = command.userId();
        }

        if (evaluatedGameIds.contains(command.gameId())) {
            return;
        }

        Instant now = Instant.now();
        apply(new ProgressionEvent.BadgeFactsRecordedEvent(
                command.badgeId(),
                command.userId(),
                command.gameId(),
                command.topicId(),
                command.xp(),
                command.gameScore(),
                command.won(),
                command.botGame(),
                command.fastAnswers(),
                now,
                command.activeDay()
        ));

        // Les compteurs sont à jour : l'évaluation (et le replay) portent sur le même état.
        BadgeRules.DuelFacts facts = new BadgeRules.DuelFacts(
                command.gameScore(),
                command.won(),
                command.fastAnswers(),
                humanDuels,
                humanWins,
                xpByTopic.getOrDefault(command.topicId(), 0),
                winStreakByTopic.getOrDefault(command.topicId(), 0),
                (int) xpByTopic.values().stream().filter(value -> value > 0).count(),
                perfectDuels,
                fastAnswersTotal,
                activity.longestStreak()
        );

        // Ordre stable (règles) : le flux d'événements reste déterministe au replay.
        for (Badge badge : BadgeRules.evaluate(facts, badges)) {
            apply(new ProgressionEvent.BadgeEarnedEvent(command.userId(), badge, now));
        }
    }

    /**
     * Compteur **hors duel** (abonné gagné, défi lancé) : incrémente le compteur de la métrique
     * puis réévalue les badges. La livraison est au-moins-une-fois (saga) : un rejeu ne peut que
     * faire progresser un badge cosmétique, jamais le retirer.
     */
    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(ProgressionCommand.RecordBadgeCounterCommand command) {
        if (this.badgeId == null) {
            this.badgeId = command.badgeId();
        }
        if (this.userId == null) {
            this.userId = command.userId();
        }

        Instant now = Instant.now();
        apply(new ProgressionEvent.BadgeCounterRecordedEvent(
                command.badgeId(),
                command.userId(),
                command.metric(),
                command.sourceId(),
                now
        ));

        // Compteurs à jour : l'évaluation (et le replay) portent sur le même état.
        BadgeRules.CounterFacts facts =
                new BadgeRules.CounterFacts(followersGained, challengesSent);
        for (Badge badge : BadgeRules.evaluateCounters(facts, badges)) {
            apply(new ProgressionEvent.BadgeEarnedEvent(command.userId(), badge, now));
        }
    }

    @EventSourcingHandler
    public void on(ProgressionEvent.BadgeCounterRecordedEvent event) {
        this.badgeId = event.badgeId();
        this.userId = event.userId();
        switch (event.metric()) {
            case FOLLOWERS_GAINED -> this.followersGained++;
            case CHALLENGES_SENT -> this.challengesSent++;
            default -> {
                // Métriques alimentées par les duels : rien à faire ici.
            }
        }
    }

    @EventSourcingHandler
    public void on(ProgressionEvent.BadgeFactsRecordedEvent event) {
        this.badgeId = event.badgeId();
        this.userId = event.userId();
        this.evaluatedGameIds.add(event.gameId());
        this.activity = ActivityRules.advance(this.activity, event.activeDay());
        this.xpByTopic.merge(event.topicId(), event.xp(), Integer::sum);

        // Faits d'exploit du duel : comptés même en entraînement (comme PERFECT/ÉCLAIR).
        if (event.gameScore() >= ProgressionRules.PERFECT_SCORE) {
            this.perfectDuels++;
        }
        this.fastAnswersTotal += event.fastAnswers();

        // Volume et maîtrise : seuls les duels entre joueurs comptent.
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
    public void on(ProgressionEvent.BadgeEarnedEvent event) {
        this.badges.add(event.badge());
    }
}
