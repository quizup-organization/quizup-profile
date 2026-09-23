package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.exception.ProfileProblems;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * Test Axon in-memory de l'agrégat {@link ProfileAggregate} via {@link AggregateTestFixture}.
 * <p>
 * 100 % in-memory : event store de l'agrégat en mémoire, aucun Postgres ni Axon Server.
 */
class ProfileAggregateTest {

    private final AggregateTestFixture<ProfileAggregate> fixture =
            new AggregateTestFixture<>(ProfileAggregate.class);

    @Test
    void create_appliesProfileCreatedEvent() {
        fixture
                .givenNoPriorActivity()
                .when(new ProfileCommand.CreateProfileCommand("user-1", "user@quizup.dev", "Alice"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfileCreatedEvent.class,
                        e -> "user-1".equals(((ProfileEvent.ProfileCreatedEvent) e).userId())
                                && "user@quizup.dev".equals(((ProfileEvent.ProfileCreatedEvent) e).email())
                                && "Alice".equals(((ProfileEvent.ProfileCreatedEvent) e).displayName())));
    }

    @Test
    void createWithBlankDisplayName_throwsValidationProblem() {
        fixture
                .givenNoPriorActivity()
                .when(new ProfileCommand.CreateProfileCommand("user-1", "user@quizup.dev", "  "))
                .expectException(ProfileProblems.DisplayNameBlankProblem.class);
    }

    @Test
    void createWithTooLongDisplayName_throwsValidationProblem() {
        fixture
                .givenNoPriorActivity()
                .when(new ProfileCommand.CreateProfileCommand(
                        "user-1",
                        "user@quizup.dev",
                        "x".repeat(101)))
                .expectException(ProfileProblems.DisplayNameTooLongProblem.class);
    }

    @Test
    void updateByOwner_appliesProfileUpdatedEvent() {
        fixture
                .given(new ProfileEvent.ProfileCreatedEvent("user-1", "user@quizup.dev", "Alice", Instant.now()))
                .when(new ProfileCommand.UpdateProfileCommand(
                        "user-1",
                        "user-1",
                        "Alicia",
                        "Full stack developer",
                        "France",
                        "{\"hairVariant\":[\"full\"]}"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfileUpdatedEvent.class,
                        e -> "user-1".equals(((ProfileEvent.ProfileUpdatedEvent) e).userId())
                                && "user-1".equals(((ProfileEvent.ProfileUpdatedEvent) e).requestedBy())
                                && "Alicia".equals(((ProfileEvent.ProfileUpdatedEvent) e).displayName())
                                && "Full stack developer".equals(((ProfileEvent.ProfileUpdatedEvent) e).bio())
                                && "France".equals(((ProfileEvent.ProfileUpdatedEvent) e).country())
                                && "{\"hairVariant\":[\"full\"]}".equals(((ProfileEvent.ProfileUpdatedEvent) e).avatarOptions())));
    }


    @Test
    void updateByNonOwner_isRejected() {
        fixture
                .given(new ProfileEvent.ProfileCreatedEvent("user-1", "user@quizup.dev", "Alice", Instant.now()))
                .when(new ProfileCommand.UpdateProfileCommand(
                        "user-1",
                        "user-2",
                        "Alicia",
                        null,
                        null,
                        null))
                .expectException(ProfileProblems.ProfileNotOwnerProblem.class);
    }


    @Test
    void updateWithBlankDisplayName_throwsValidationProblem() {
        fixture
                .given(new ProfileEvent.ProfileCreatedEvent("user-1", "user@quizup.dev", "Alice", Instant.now()))
                .when(new ProfileCommand.UpdateProfileCommand(
                        "user-1",
                        "user-1",
                        "",
                        null,
                        null,
                        null))
                .expectException(ProfileProblems.DisplayNameBlankProblem.class);
    }

    @Test
    void updateWithTooLongBio_throwsValidationProblem() {
        fixture
                .given(new ProfileEvent.ProfileCreatedEvent("user-1", "user@quizup.dev", "Alice", Instant.now()))
                .when(new ProfileCommand.UpdateProfileCommand(
                        "user-1",
                        "user-1",
                        "Alice",
                        "x".repeat(301),
                        null,
                        null))
                .expectException(ProfileProblems.BioTooLongProblem.class);
    }

    @Test
    void updateWithTooLongCountry_throwsValidationProblem() {
        fixture
                .given(new ProfileEvent.ProfileCreatedEvent("user-1", "user@quizup.dev", "Alice", Instant.now()))
                .when(new ProfileCommand.UpdateProfileCommand(
                        "user-1",
                        "user-1",
                        "Alice",
                        null,
                        "x".repeat(101),
                        null))
                .expectException(ProfileProblems.CountryTooLongProblem.class);
    }

    @Test
    void updateWithTooLongAvatarOptions_throwsValidationProblem() {
        fixture
                .given(new ProfileEvent.ProfileCreatedEvent("user-1", "user@quizup.dev", "Alice", Instant.now()))
                .when(new ProfileCommand.UpdateProfileCommand(
                        "user-1",
                        "user-1",
                        "Alice",
                        null,
                        null,
                        "x".repeat(2001)))
                .expectException(ProfileProblems.AvatarOptionsTooLongProblem.class);
    }
}
