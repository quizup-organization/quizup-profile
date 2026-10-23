package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.command.RewardCommand;
import io.github.quizup.profile.domain.event.RewardEvent;
import io.github.quizup.profile.domain.model.RewardCode;
import io.github.quizup.profile.domain.model.RewardRules;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.HashSet;
import java.util.Set;

/**
 * PlayerRewardAggregate — récompenses persistées d'un joueur ({@code rewards:<userId>}).
 *
 * <p>Le déblocage est idempotent : une récompense déjà possédée n'émet aucun nouvel événement.
 * Les récompenses sont **définitives** : aucun handler ne les retire.</p>
 */
@Aggregate
public class PlayerRewardAggregate {

    @AggregateIdentifier
    private String rewardId;

    private String userId;

    private final Set<RewardCode> rewards = new HashSet<>();

    protected PlayerRewardAggregate() {
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(RewardCommand.GrantRewardCommand command) {
        if (this.rewardId == null) {
            this.rewardId = command.rewardId();
        }
        if (this.userId == null) {
            this.userId = command.userId();
        }
        if (rewards.contains(command.rewardCode())) {
            return;
        }

        AggregateLifecycle.apply(new RewardEvent.RewardGrantedEvent(
                command.userId(),
                command.rewardCode(),
                command.source(),
                command.unlockedAt()
        ));
    }

    @EventSourcingHandler
    public void on(RewardEvent.RewardGrantedEvent event) {
        this.rewardId = RewardRules.rewardIdFor(event.userId());
        this.userId = event.userId();
        this.rewards.add(event.rewardCode());
    }
}
