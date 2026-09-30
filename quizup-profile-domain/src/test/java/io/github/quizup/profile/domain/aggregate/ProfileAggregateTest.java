package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
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

    private static final Instant NOW = Instant.parse("2026-09-20T10:00:00Z");

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
                                && "Alice".equals(((ProfileEvent.ProfileCreatedEvent) e).pseudonym())));
    }

    @Test
    void createWithBlankPseudonym_throwsValidationProblem() {
        fixture
                .givenNoPriorActivity()
                .when(new ProfileCommand.CreateProfileCommand("user-1", "user@quizup.dev", "  "))
                .expectException(ProfileProblems.PseudonymBlankProblem.class);
    }

    @Test
    void createWithTooLongPseudonym_throwsValidationProblem() {
        fixture
                .givenNoPriorActivity()
                .when(new ProfileCommand.CreateProfileCommand(
                        "user-1",
                        "user@quizup.dev",
                        "x".repeat(101)))
                .expectException(ProfileProblems.PseudonymTooLongProblem.class);
    }

    @Test
    void updatePseudonymByOwner_appliesPseudonymUpdatedEvent() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfilePseudonymCommand("user-1", "user-1", "Alicia"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfilePseudonymUpdatedEvent.class,
                        e -> "user-1".equals(((ProfileEvent.ProfilePseudonymUpdatedEvent) e).userId())
                                && "user-1".equals(((ProfileEvent.ProfilePseudonymUpdatedEvent) e).requestedBy())
                                && "Alicia".equals(((ProfileEvent.ProfilePseudonymUpdatedEvent) e).pseudonym())));
    }

    @Test
    void updatePseudonymWithSameValue_emitsNoEvent() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfilePseudonymCommand("user-1", "user-1", "Alice"))
                .expectNoEvents();
    }

    @Test
    void updateByNonOwner_isRejected() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfilePseudonymCommand("user-1", "user-2", "Alicia"))
                .expectException(ProfileProblems.ProfileNotOwnerProblem.class);
    }

    @Test
    void updatePseudonymWithBlank_throwsValidationProblem() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfilePseudonymCommand("user-1", "user-1", ""))
                .expectException(ProfileProblems.PseudonymBlankProblem.class);
    }

    @Test
    void updateBio_appliesBioUpdatedEvent() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileBioCommand("user-1", "user-1", "Full stack developer"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfileBioUpdatedEvent.class,
                        e -> "Full stack developer".equals(((ProfileEvent.ProfileBioUpdatedEvent) e).bio())));
    }

    @Test
    void updateBioWithSameValue_emitsNoEvent() {
        fixture
                .given(createdProfile(),
                        new ProfileEvent.ProfileBioUpdatedEvent("user-1", "user-1", "Full stack developer", NOW))
                .when(new ProfileCommand.UpdateProfileBioCommand("user-1", "user-1", "Full stack developer"))
                .expectNoEvents();
    }

    @Test
    void updateBioTooLong_throwsValidationProblem() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileBioCommand("user-1", "user-1", "x".repeat(301)))
                .expectException(ProfileProblems.BioTooLongProblem.class);
    }

    @Test
    void updateCountry_appliesCountryUpdatedEvent() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileCountryCommand("user-1", "user-1", "France"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfileCountryUpdatedEvent.class,
                        e -> "France".equals(((ProfileEvent.ProfileCountryUpdatedEvent) e).country())));
    }

    @Test
    void updateCountryTooLong_throwsValidationProblem() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileCountryCommand("user-1", "user-1", "x".repeat(101)))
                .expectException(ProfileProblems.CountryTooLongProblem.class);
    }

    @Test
    void updateAvatar_appliesAvatarUpdatedEvent() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileAvatarCommand("user-1", "user-1", "{\"hairVariant\":[\"full\"]}"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfileAvatarUpdatedEvent.class,
                        e -> "{\"hairVariant\":[\"full\"]}"
                                .equals(((ProfileEvent.ProfileAvatarUpdatedEvent) e).avatarOptions())));
    }

    @Test
    void updateAvatarTooLong_throwsValidationProblem() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileAvatarCommand("user-1", "user-1", "x".repeat(2001)))
                .expectException(ProfileProblems.AvatarOptionsTooLongProblem.class);
    }

    @Test
    void updateLanguage_appliesLanguageUpdatedEvent() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileLanguageCommand("user-1", "user-1", Language.EN))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        ProfileEvent.ProfileLanguageUpdatedEvent.class,
                        e -> Language.EN == ((ProfileEvent.ProfileLanguageUpdatedEvent) e).language()));
    }

    @Test
    void updateLanguageWithSameValue_emitsNoEvent() {
        fixture
                .given(createdProfile(),
                        new ProfileEvent.ProfileLanguageUpdatedEvent("user-1", "user-1", Language.EN, NOW))
                .when(new ProfileCommand.UpdateProfileLanguageCommand("user-1", "user-1", Language.EN))
                .expectNoEvents();
    }

    @Test
    void updateLanguageWithNull_throwsValidationProblem() {
        fixture
                .given(createdProfile())
                .when(new ProfileCommand.UpdateProfileLanguageCommand("user-1", "user-1", null))
                .expectException(ProfileProblems.LanguageMissingProblem.class);
    }

    private ProfileEvent.ProfileCreatedEvent createdProfile() {
        return new ProfileEvent.ProfileCreatedEvent("user-1", "user@quizup.dev", "Alice", NOW);
    }
}
