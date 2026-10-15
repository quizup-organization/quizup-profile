package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import io.github.quizup.profile.domain.command.ProgressionCommand;
import io.github.quizup.profile.domain.event.ProgressionEvent;
import io.github.quizup.profile.domain.model.Badge;
import io.github.quizup.profile.domain.model.BadgeMetric;
import io.github.quizup.profile.domain.model.ProgressionRules;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Test Axon in-memory de {@link BadgeAggregate} : évaluation des badges (exploits, volume,
 * maîtrise, séries, exploration, collection), idempotence par duel et portée « duels entre
 * joueurs » (l'entraînement n'alimente pas le volume/la maîtrise mais reste évalué pour les
 * exploits).
 */
class BadgeAggregateTest {

    private static final String BADGE_ID = ProgressionRules.badgeIdFor("user-1");

    private final AggregateTestFixture<BadgeAggregate> fixture =
            new AggregateTestFixture<>(BadgeAggregate.class);

    @Test
    void win_earnsFirstWinBadge() {
        fixture.givenNoPriorActivity()
                .when(duel("game-1", "topic-1", 40, 40, true, false, 2))
                .expectEventsMatching(earned(Badge.FIRST_WIN));
    }

    @Test
    void botGame_stillEvaluatesExploitBadges() {
        fixture.givenNoPriorActivity()
                .when(duel("game-1", "topic-1", 0, 40, true, true, 2))
                .expectEventsMatching(earned(Badge.FIRST_WIN));
    }

    @Test
    void perfectScore_earnsPerfectBadge() {
        fixture.givenNoPriorActivity()
                .when(duel("game-1", "topic-1", 160, 160, true, false, 2))
                .expectEventsMatching(earned(Badge.PERFECT));
    }

    @Test
    void fiveFastAnswersInOneDuel_earnsLightningBadge() {
        fixture.givenNoPriorActivity()
                .when(duel("game-1", "topic-1", 40, 40, false, false, 5))
                .expectEventsMatching(earned(Badge.LIGHTNING));
    }

    @Test
    void tenthHumanDuel_earnsDuelistBronze() {
        fixture.given(humanDuels(9))
                .when(duel("game-10", "topic-1", 30, 30, false, false, 0))
                .expectEventsMatching(earned(Badge.DUELIST_BRONZE));
    }

    @Test
    void botDuels_doNotAdvanceHumanVolume() {
        fixture.given(botDuels(9))
                .when(duel("game-10", "topic-1", 0, 30, false, true, 0))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        ProgressionEvent.BadgeFactsRecordedEvent.class,
                        e -> "game-10".equals(((ProgressionEvent.BadgeFactsRecordedEvent) e).gameId())));
    }

    @Test
    void masteryThreshold_earnsMasterBronze() {
        fixture.given(new ProgressionEvent.BadgeFactsRecordedEvent(
                        BADGE_ID, "user-1", "game-1", "topic-1", 2_400, 160, true, false, 2,
                        0, Instant.now(), null))
                .when(duel("game-2", "topic-1", 150, 40, true, false, 2))
                .expectEventsMatching(earned(Badge.MASTER_BRONZE));
    }

    @Test
    void fourthBadgeThenANewOne_earnsCollectorBronze() {
        fixture.given(
                        new ProgressionEvent.BadgeFactsRecordedEvent(
                                BADGE_ID, "user-1", "game-0", "topic-1", 10, 10, false, false, 0,
                                0, Instant.now(), null),
                        new ProgressionEvent.BadgeEarnedEvent("user-1", Badge.PERFECT, Instant.now()),
                        new ProgressionEvent.BadgeEarnedEvent("user-1", Badge.LIGHTNING, Instant.now()),
                        new ProgressionEvent.BadgeEarnedEvent("user-1", Badge.STREAK_MASTER, Instant.now()),
                        new ProgressionEvent.BadgeEarnedEvent("user-1", Badge.DUELIST_BRONZE, Instant.now()))
                .when(duel("game-1", "topic-1", 30, 30, true, false, 0))
                .expectEventsMatching(earned(Badge.COLLECTOR_BRONZE));
    }

    @Test
    void winStreak_reachesSeriesTiers() {
        // Deux victoires consécutives sur le même sujet → la 3e fait tomber « En série ».
        fixture.given(
                        new ProgressionEvent.BadgeFactsRecordedEvent(
                                BADGE_ID, "user-1", "game-1", "topic-1", 20, 30, true, false, 0,
                                0, Instant.now(), null),
                        new ProgressionEvent.BadgeFactsRecordedEvent(
                                BADGE_ID, "user-1", "game-2", "topic-1", 20, 30, true, false, 0,
                                0, Instant.now(), null))
                .when(duel("game-3", "topic-1", 20, 30, true, false, 0))
                .expectEventsMatching(earned(Badge.SERIES_BRONZE));
    }

    @Test
    void tenthPerfectDuel_earnsPrecisionBronze() {
        fixture.given(perfectDuels(9))
                .when(duel("game-10", "topic-1", 160, 160, false, false, 0))
                .expectEventsMatching(earned(Badge.PRECISION_BRONZE));
    }

    @Test
    void fiftyFastAnswers_earnsSpeedBronze() {
        fixture.given(new ProgressionEvent.BadgeFactsRecordedEvent(
                        BADGE_ID, "user-1", "game-1", "topic-1", 30, 30, false, false, 45,
                        0, Instant.now(), null))
                .when(duel("game-2", "topic-1", 30, 30, false, false, 5))
                .expectEventsMatching(earned(Badge.SPEED_BRONZE));
    }

    @Test
    void threeConsecutiveDays_earnAssiduityBronze() {
        // Deux jours consécutifs déjà joués : le 3e jour fait tomber « Régulier ».
        LocalDate day = LocalDate.of(2026, 3, 1);
        fixture.given(
                        new ProgressionEvent.BadgeFactsRecordedEvent(
                                BADGE_ID, "user-1", "game-1", "topic-1", 10, 10, false, false, 0,
                                0, Instant.now(), day),
                        new ProgressionEvent.BadgeFactsRecordedEvent(
                                BADGE_ID, "user-1", "game-2", "topic-1", 10, 10, false, false, 0,
                                0, Instant.now(), day.plusDays(1)))
                .when(duelOnDay(day.plusDays(2), "game-3", "topic-1", 10, 10, false, false, 0))
                .expectEventsMatching(earned(Badge.ASSIDUITY_BRONZE));
    }

    @Test
    void aDayGap_restartsTheStreak() {
        // Série rompue (veille + 2 jours) : le jour suivant repart à 1, pas de badge.
        LocalDate day = LocalDate.of(2026, 3, 1);
        fixture.given(
                        new ProgressionEvent.BadgeFactsRecordedEvent(
                                BADGE_ID, "user-1", "game-1", "topic-1", 10, 10, true, false, 0,
                                0, Instant.now(), day),
                        new ProgressionEvent.BadgeFactsRecordedEvent(
                                BADGE_ID, "user-1", "game-2", "topic-1", 10, 10, true, false, 0,
                                0, Instant.now(), day.plusDays(1)))
                .when(duelOnDay(day.plusDays(5), "game-3", "topic-1", 10, 10, false, false, 0))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        ProgressionEvent.BadgeFactsRecordedEvent.class,
                        e -> "game-3".equals(
                                ((ProgressionEvent.BadgeFactsRecordedEvent) e).gameId())));
    }

    @Test
    void fifteenthImageAnswer_earnsVisionBronze() {
        fixture.given(imageAnswers(14))
                .when(duelWith("game-15", "topic-1", 10, 10, false, false, 0, 1, null))
                .expectEventsMatching(earned(Badge.VISION_BRONZE));
    }

    @Test
    void fiveFollowers_earnSocialBronze() {
        fixture.given(followers(4))
                .when(follower("follow-5"))
                .expectEventsMatching(earned(Badge.SOCIAL_BRONZE));
    }

    @Test
    void firstChallenge_earnsChallengesBronze() {
        fixture.givenNoPriorActivity()
                .when(new ProgressionCommand.RecordBadgeCounterCommand(
                        BADGE_ID, "user-1", BadgeMetric.CHALLENGES_SENT, "challenge-1",
                        Instant.now()))
                .expectEventsMatching(earned(Badge.CHALLENGES_BRONZE));
    }

    @Test
    void exploringTopics_earnsExplorerBronze() {
        fixture.given(distinctTopics(4))
                .when(duel("game-5", "topic-5", 30, 30, false, false, 0))
                .expectEventsMatching(earned(Badge.EXPLORER_BRONZE));
    }

    @Test
    void sameGameId_isIdempotent() {
        fixture.given(new ProgressionEvent.BadgeFactsRecordedEvent(
                        BADGE_ID, "user-1", "game-1", "topic-1", 100, 100, false, false, 2,
                        0, Instant.now(), null))
                .when(duel("game-1", "topic-1", 100, 100, true, false, 2))
                .expectNoEvents();
    }

    @Test
    void alreadyEarnedFirstWin_isNotReapplied() {
        fixture.given(
                        new ProgressionEvent.BadgeFactsRecordedEvent(
                                BADGE_ID, "user-1", "game-1", "topic-1", 100, 100, true, false, 2,
                                0, Instant.now(), null),
                        new ProgressionEvent.BadgeEarnedEvent("user-1", Badge.FIRST_WIN, Instant.now()))
                .when(duel("game-2", "topic-1", 30, 30, true, false, 2))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        ProgressionEvent.BadgeFactsRecordedEvent.class,
                        e -> "game-2".equals(((ProgressionEvent.BadgeFactsRecordedEvent) e).gameId())));
    }

    private static ProgressionCommand.EvaluateBadgesCommand duelOnDay(
            LocalDate day, String gameId, String topicId, int xp, int score, boolean won,
            boolean bot, int fast) {
        return duelWith(gameId, topicId, xp, score, won, bot, fast, 0, day);
    }

    private static ProgressionCommand.EvaluateBadgesCommand duel(
            String gameId, String topicId, int xp, int score, boolean won, boolean bot, int fast) {
        return duelWith(gameId, topicId, xp, score, won, bot, fast, 0, null);
    }

    private static ProgressionCommand.EvaluateBadgesCommand duelWith(
            String gameId, String topicId, int xp, int score, boolean won, boolean bot, int fast,
            int imageCorrect, LocalDate day) {
        return new ProgressionCommand.EvaluateBadgesCommand(
                BADGE_ID, "user-1", gameId, topicId, xp, score, won, bot, fast, imageCorrect, day);
    }

    private static Matcher<? extends java.util.List<? super EventMessage<?>>> earned(Badge badge) {
        return QuizUpAxonMatchers.hasPayloadMatching(
                ProgressionEvent.BadgeEarnedEvent.class,
                e -> ((ProgressionEvent.BadgeEarnedEvent) e).badge() == badge);
    }

    private static ProgressionEvent.BadgeFactsRecordedEvent[] humanDuels(int count) {
        ProgressionEvent.BadgeFactsRecordedEvent[] duels =
                new ProgressionEvent.BadgeFactsRecordedEvent[count];
        for (int index = 0; index < count; index++) {
            duels[index] = new ProgressionEvent.BadgeFactsRecordedEvent(
                    BADGE_ID, "user-1", "game-" + index, "topic-1", 10, 10, false, false, 0,
                    0, Instant.now(), null);
        }
        return duels;
    }

    private static ProgressionEvent.BadgeFactsRecordedEvent[] botDuels(int count) {
        ProgressionEvent.BadgeFactsRecordedEvent[] duels =
                new ProgressionEvent.BadgeFactsRecordedEvent[count];
        for (int index = 0; index < count; index++) {
            duels[index] = new ProgressionEvent.BadgeFactsRecordedEvent(
                    BADGE_ID, "user-1", "bot-" + index, "topic-1", 0, 10, false, true, 0,
                    0, Instant.now(), null);
        }
        return duels;
    }

    private static ProgressionCommand.RecordBadgeCounterCommand follower(String sourceId) {
        return new ProgressionCommand.RecordBadgeCounterCommand(
                BADGE_ID, "user-1", BadgeMetric.FOLLOWERS_GAINED, sourceId, Instant.now());
    }

    private static ProgressionEvent.BadgeFactsRecordedEvent[] imageAnswers(int count) {
        ProgressionEvent.BadgeFactsRecordedEvent[] duels =
                new ProgressionEvent.BadgeFactsRecordedEvent[count];
        for (int index = 0; index < count; index++) {
            duels[index] = new ProgressionEvent.BadgeFactsRecordedEvent(
                    BADGE_ID, "user-1", "image-" + index, "topic-1", 10, 10, false, false, 0, 1,
                    Instant.now(), null);
        }
        return duels;
    }

    private static ProgressionEvent.BadgeCounterRecordedEvent[] followers(int count) {
        ProgressionEvent.BadgeCounterRecordedEvent[] events =
                new ProgressionEvent.BadgeCounterRecordedEvent[count];
        for (int index = 0; index < count; index++) {
            events[index] = new ProgressionEvent.BadgeCounterRecordedEvent(
                    BADGE_ID, "user-1", BadgeMetric.FOLLOWERS_GAINED, "follow-" + index,
                    Instant.now());
        }
        return events;
    }

    private static ProgressionEvent.BadgeFactsRecordedEvent[] perfectDuels(int count) {
        ProgressionEvent.BadgeFactsRecordedEvent[] duels =
                new ProgressionEvent.BadgeFactsRecordedEvent[count];
        for (int index = 0; index < count; index++) {
            duels[index] = new ProgressionEvent.BadgeFactsRecordedEvent(
                    BADGE_ID, "user-1", "perfect-" + index, "topic-1", 160, 160, true, false, 0,
                    0, Instant.now(), null);
        }
        return duels;
    }

    private static ProgressionEvent.BadgeFactsRecordedEvent[] distinctTopics(int count) {
        ProgressionEvent.BadgeFactsRecordedEvent[] duels =
                new ProgressionEvent.BadgeFactsRecordedEvent[count];
        for (int index = 0; index < count; index++) {
            duels[index] = new ProgressionEvent.BadgeFactsRecordedEvent(
                    BADGE_ID, "user-1", "game-" + index, "topic-" + index, 30, 30, false, false, 0,
                    0, Instant.now(), null);
        }
        return duels;
    }
}
