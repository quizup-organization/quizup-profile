package io.github.quizup.profile.application.handler.event;

import io.github.quizup.profile.domain.command.RewardCommand;
import io.github.quizup.profile.domain.event.ProgressionEvent;
import io.github.quizup.profile.domain.model.RewardRules;
import io.github.quizup.profile.domain.model.RewardSource;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

/**
 * Débloque les récompenses liées aux badges. Le badge reste une réussite autonome ; ce handler
 * ne fait que traduire un badge en récompense via {@link RewardRules}.
 */
@Component
@ProcessingGroup("reward-grant")
public class BadgeRewardHandler {

    private final CommandGateway commandGateway;

    public BadgeRewardHandler(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @EventHandler
    public void on(ProgressionEvent.BadgeEarnedEvent event) {
        RewardRules.forBadge(event.badge()).ifPresent(rewardCode ->
                commandGateway.sendAndWait(new RewardCommand.GrantRewardCommand(
                        RewardRules.rewardIdFor(event.userId()),
                        event.userId(),
                        rewardCode,
                        RewardSource.BADGE,
                        event.earnedAt()
                )));
    }
}
