package io.github.quizup.profile.application.handler.event;

import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.event.ProgressionEvent;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * La première partie humaine terminée vaut onboarding implicite : un joueur invité dans une
 * room privée a joué avant de voir le flow, il n'a plus besoin de celui-ci. Le bot et les
 * parties annulées n'émettent pas d'XP et ne déclenchent donc pas ce marquage.
 */
@Component
@ProcessingGroup("onboarding-completion")
public class OnboardingCompletionHandler {

    private final CommandGateway commandGateway;

    public OnboardingCompletionHandler(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @EventHandler
    public void on(ProgressionEvent.XpAwardedEvent event) {
        if (event.botGame()) {
            return;
        }
        commandGateway.send(new ProfileCommand.CompleteOnboardingCommand(
                event.userId(), Instant.now()));
    }
}
