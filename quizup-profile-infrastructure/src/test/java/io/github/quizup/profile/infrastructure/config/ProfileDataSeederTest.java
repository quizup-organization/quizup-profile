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
    void runSeedsSystemProfileWhenEnabledAndAbsent() {
        ReflectionTestUtils.setField(seeder, "seedDataEnabled", true);
        when(checkProfileUseCase.existsById(anyString()))
                .thenReturn(CompletableFuture.completedFuture(false));
        when(createProfileUseCase.create(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("created"));

        seeder.run();

        verify(createProfileUseCase).create(
                eq(QuizUpConstants.SYSTEM_USER_ID),
                eq(QuizUpConstants.SYSTEM_USER_EMAIL),
                eq(QuizUpConstants.SYSTEM_USER_NAME));
    }

    @Test
    void runDoesNothingWhenDisabled() {
        ReflectionTestUtils.setField(seeder, "seedDataEnabled", false);

        seeder.run();

        verifyNoInteractions(createProfileUseCase, checkProfileUseCase);
    }

    @Test
    void seedProfileSkipsWhenProjectionKnowsProfile() {
        when(checkProfileUseCase.existsById(QuizUpConstants.SYSTEM_USER_ID))
                .thenReturn(CompletableFuture.completedFuture(true));

        seeder.seedProfile(
                QuizUpConstants.SYSTEM_USER_ID,
                QuizUpConstants.SYSTEM_USER_EMAIL,
                QuizUpConstants.SYSTEM_USER_NAME,
                "System");

        verify(createProfileUseCase, never()).create(anyString(), anyString(), anyString());
    }

    @Test
    void seedProfileIgnoresExistingAggregateWhenProjectionLagging() {
        when(checkProfileUseCase.existsById(QuizUpConstants.SYSTEM_USER_ID))
                .thenReturn(CompletableFuture.completedFuture(false));
        when(createProfileUseCase.create(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(
                        new AggregateStreamCreationException(QuizUpConstants.SYSTEM_USER_ID)));

        assertThatCode(() -> seeder.seedProfile(
                QuizUpConstants.SYSTEM_USER_ID,
                QuizUpConstants.SYSTEM_USER_EMAIL,
                QuizUpConstants.SYSTEM_USER_NAME,
                "System")).doesNotThrowAnyException();
    }

    @Test
    void seedProfileCreatesWhenAbsent() {
        when(checkProfileUseCase.existsById(QuizUpConstants.SYSTEM_USER_ID))
                .thenReturn(CompletableFuture.completedFuture(false));
        when(createProfileUseCase.create(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("created"));

        seeder.seedProfile(
                QuizUpConstants.SYSTEM_USER_ID,
                QuizUpConstants.SYSTEM_USER_EMAIL,
                QuizUpConstants.SYSTEM_USER_NAME,
                "System");

        verify(createProfileUseCase).create(
                eq(QuizUpConstants.SYSTEM_USER_ID),
                eq(QuizUpConstants.SYSTEM_USER_EMAIL),
                eq(QuizUpConstants.SYSTEM_USER_NAME));
    }
}
