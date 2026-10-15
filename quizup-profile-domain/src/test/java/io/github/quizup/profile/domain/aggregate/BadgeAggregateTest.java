package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import io.github.quizup.profile.domain.command.ProgressionCommand;
import io.github.quizup.profile.domain.event.ProgressionEvent;
import io.github.quizup.profile.domain.model.Badge;
import io.github.quizup.profile.domain.model.ProgressionRules;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.time.Instant;

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
                        Instant.now()))
                .when(duel("game-2", "topic-1", 150, 40, true, false, 2))
                .expectEventsMatching(earned(Badge.MASTER_BRONZE));
    }

    @Test
    void fourthBadgeThenANewOne_earnsCollectorBronze() {
        fixture.given(
                        new ProgressionEvent.BadgeFactsRecordedEvent(
                                BADGE_ID, "user-1", "game-0", "topic-1", 10, 10, false, false, 0,
                                Instant.now()),
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
                                Instant.now()),
                        new ProgressionEvent.BadgeFactsRecordedEvent(
                                BADGE_ID, "user-1", "game-2", "topic-1", 20, 30, true, false, 0,
                                Instant.now()))
                .when(duel("game-3", "topic-1", 20, 30, true, false, 0))
                .expectEventsMatching(earned(Badge.SERIES_BRONZE));
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
                        Instant.now()))
                .when(duel("game-1", "topic-1", 100, 100, true, false, 2))
                .expectNoEvents();
    }

    @Test
    void alreadyEarnedFirstWin_isNotReapplied() {
        fixture.given(
                        new ProgressionEvent.BadgeFactsRecordedEvent(
                                BADGE_ID, "user-1", "game-1", "topic-1", 100, 100, true, false, 2,
                                Instant.now()),
                        new ProgressionEvent.BadgeEarnedEvent("user-1", Badge.FIRST_WIN, Instant.now()))
                .when(duel("game-2", "topic-1", 30, 30, true, false, 2))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        ProgressionEvent.BadgeFactsRecordedEvent.class,
                        e -> "game-2".equals(((ProgressionEvent.BadgeFactsRecordedEvent) e).gameId())));
    }

    private static ProgressionCommand.EvaluateBadgesCommand duel(
            String gameId, String topicId, int xp, int score, boolean won, boolean bot, int fast) {
        return new ProgressionCommand.EvaluateBadgesCommand(
                BADGE_ID, "user-1", gameId, topicId, xp, score, won, bot, fast);
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
                    Instant.now());
        }
        return duels;
    }

    private static ProgressionEvent.BadgeFactsRecordedEvent[] botDuels(int count) {
        ProgressionEvent.BadgeFactsRecordedEvent[] duels =
                new ProgressionEvent.BadgeFactsRecordedEvent[count];
        for (int index = 0; index < count; index++) {
            duels[index] = new ProgressionEvent.BadgeFactsRecordedEvent(
                    BADGE_ID, "user-1", "bot-" + index, "topic-1", 0, 10, false, true, 0,
                    Instant.now());
        }
        return duels;
    }

    private static ProgressionEvent.BadgeFactsRecordedEvent[] distinctTopics(int count) {
        ProgressionEvent.BadgeFactsRecordedEvent[] duels =
                new ProgressionEvent.BadgeFactsRecordedEvent[count];
        for (int index = 0; index < count; index++) {
            duels[index] = new ProgressionEvent.BadgeFactsRecordedEvent(
                    BADGE_ID, "user-1", "game-" + index, "topic-" + index, 30, 30, false, false, 0,
                    Instant.now());
        }
        return duels;
    }
}
