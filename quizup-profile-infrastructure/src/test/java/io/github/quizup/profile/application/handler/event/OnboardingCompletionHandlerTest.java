package io.github.quizup.profile.application.handler.event;

import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.event.ProgressionEvent;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OnboardingCompletionHandlerTest {

    private final CommandGateway commandGateway = mock(CommandGateway.class);
    private final OnboardingCompletionHandler handler = new OnboardingCompletionHandler(commandGateway);

    @Test
    void humanGameCompletesOnboarding() {
        handler.on(event(false));

        verify(commandGateway).send(any(ProfileCommand.CompleteOnboardingCommand.class));
    }

    @Test
    void botGameDoesNotCompleteOnboarding() {
        handler.on(event(true));

        verify(commandGateway, never()).send(any());
    }

    private static ProgressionEvent.XpAwardedEvent event(boolean botGame) {
        return new ProgressionEvent.XpAwardedEvent(
                "user-1", "game-1", "topic-1", 10, 100, true, false, botGame,
                8, 2, 0, Instant.parse("2026-10-23T10:00:00Z"),
                1000, 1000, 0, true, 0, null, null);
    }
}
