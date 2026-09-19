package io.github.quizup.profile.infrastructure.config;

import io.github.quizup.microservice.core.domain.constant.QuizUpConstants;
import io.github.quizup.profile.domain.port.in.CheckProfileUseCase;
import io.github.quizup.profile.domain.port.in.CreateProfileUseCase;
import org.axonframework.modelling.command.AggregateStreamCreationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProfileDataSeederTest {

    private CheckProfileUseCase checkProfileUseCase;
    private CreateProfileUseCase createProfileUseCase;
    private ProfileDataSeeder seeder;

    @BeforeEach
    void setUp() {
        checkProfileUseCase = mock(CheckProfileUseCase.class);
        createProfileUseCase = mock(CreateProfileUseCase.class);
        seeder = new ProfileDataSeeder(checkProfileUseCase, createProfileUseCase);
    }

    @Test
    void runSeedsAdminAndBotWhenEnabledAndAbsent() {
        ReflectionTestUtils.setField(seeder, "seedDataEnabled", true);
        when(checkProfileUseCase.existsById(anyString()))
                .thenReturn(CompletableFuture.completedFuture(false));
        when(createProfileUseCase.create(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("created"));

        seeder.run();

        verify(createProfileUseCase).create(
                eq(QuizUpConstants.ADMIN_USER_ID),
                eq(QuizUpConstants.ADMIN_USER_EMAIL),
                eq(QuizUpConstants.ADMIN_USER_NAME));
        verify(createProfileUseCase).create(
                eq(QuizUpConstants.BOT_USER_ID),
                eq(QuizUpConstants.BOT_USER_EMAIL),
                eq(QuizUpConstants.BOT_USER_NAME));
    }

    @Test
    void runDoesNothingWhenDisabled() {
        ReflectionTestUtils.setField(seeder, "seedDataEnabled", false);

        seeder.run();

        verifyNoInteractions(createProfileUseCase, checkProfileUseCase);
    }

    @Test
    void seedProfileSkipsWhenProjectionKnowsProfile() {
        when(checkProfileUseCase.existsById(QuizUpConstants.ADMIN_USER_ID))
                .thenReturn(CompletableFuture.completedFuture(true));

        seeder.seedProfile(
                QuizUpConstants.ADMIN_USER_ID,
                QuizUpConstants.ADMIN_USER_EMAIL,
                QuizUpConstants.ADMIN_USER_NAME,
                "Admin");

        verify(createProfileUseCase, never()).create(anyString(), anyString(), anyString());
    }

    @Test
    void seedProfileIgnoresExistingAggregateWhenProjectionLagging() {
        when(checkProfileUseCase.existsById(QuizUpConstants.ADMIN_USER_ID))
                .thenReturn(CompletableFuture.completedFuture(false));
        when(createProfileUseCase.create(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(
                        new AggregateStreamCreationException(QuizUpConstants.ADMIN_USER_ID)));

        assertThatCode(() -> seeder.seedProfile(
                QuizUpConstants.ADMIN_USER_ID,
                QuizUpConstants.ADMIN_USER_EMAIL,
                QuizUpConstants.ADMIN_USER_NAME,
                "Admin")).doesNotThrowAnyException();
    }

    @Test
    void seedProfileCreatesWhenAbsent() {
        when(checkProfileUseCase.existsById(QuizUpConstants.BOT_USER_ID))
                .thenReturn(CompletableFuture.completedFuture(false));
        when(createProfileUseCase.create(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("created"));

        seeder.seedProfile(
                QuizUpConstants.BOT_USER_ID,
                QuizUpConstants.BOT_USER_EMAIL,
                QuizUpConstants.BOT_USER_NAME,
                "Bot");

        verify(createProfileUseCase).create(
                eq(QuizUpConstants.BOT_USER_ID),
                eq(QuizUpConstants.BOT_USER_EMAIL),
                eq(QuizUpConstants.BOT_USER_NAME));
    }
}
