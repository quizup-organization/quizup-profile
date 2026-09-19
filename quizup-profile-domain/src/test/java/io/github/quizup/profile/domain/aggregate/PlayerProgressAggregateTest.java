package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import io.github.quizup.profile.domain.command.ProgressionCommand;
import io.github.quizup.profile.domain.event.ProgressionEvent;
import io.github.quizup.profile.domain.model.Badge;
import io.github.quizup.profile.domain.model.ProgressionRules;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * Test Axon in-memory de {@link PlayerProgressAggregate} : attribution XP,
 * idempotence, montée de niveau et badges.
 */
class PlayerProgressAggregateTest {

    private final AggregateTestFixture<PlayerProgressAggregate> fixture =
            new AggregateTestFixture<>(PlayerProgressAggregate.class);

    @Test
    void firstAward_createsAggregateAndAppliesXpAwardedEvent() {
        fixture
                .givenNoPriorActivity()
                .when(new ProgressionCommand.AwardXpCommand(ProgressionRules.progressIdFor("user-1"), "user-1", "game-1", "topic-1", 100, false, 4, 2))
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        ProgressionEvent.XpAwardedEvent.class,
                        e -> {
                            ProgressionEvent.XpAwardedEvent xp = (ProgressionEvent.XpAwardedEvent) e;
                            return "user-1".equals(xp.userId())
                                    && "game-1".equals(xp.gameId())
                                    && "topic-1".equals(xp.topicId())
                                    && xp.xp() == 100;
                        }));
    }

    @Test
    void win_earnsFirstWinBadge() {
        fixture
                .givenNoPriorActivity()
                .when(new ProgressionCommand.AwardXpCommand(ProgressionRules.progressIdFor("user-1"), "user-1", "game-1", "topic-1", 40, true, 4, 2))
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        ProgressionEvent.BadgeEarnedEvent.class,
                        e -> ((ProgressionEvent.BadgeEarnedEvent) e).badge() == Badge.FIRST_WIN));
    }

    @Test
    void perfectScore_earnsPerfectBadge() {
        fixture
                .givenNoPriorActivity()
                .when(new ProgressionCommand.AwardXpCommand(ProgressionRules.progressIdFor("user-1"), "user-1", "game-1", "topic-1", 160, true, 4, 2))
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        ProgressionEvent.BadgeEarnedEvent.class,
                        e -> ((ProgressionEvent.BadgeEarnedEvent) e).badge() == Badge.PERFECT));
    }

    @Test
    void crossingThreshold_appliesLevelReachedEvent() {
        fixture
                .givenNoPriorActivity()
                .when(new ProgressionCommand.AwardXpCommand(ProgressionRules.progressIdFor("user-1"), "user-1", "game-1", "topic-1", 100, true, 4, 2))
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        ProgressionEvent.LevelReachedEvent.class,
                        e -> ((ProgressionEvent.LevelReachedEvent) e).level() == 2));
    }

    @Test
    void sameGameId_isIdempotent() {
        fixture
                .given(new ProgressionEvent.XpAwardedEvent(
                        "user-1", "game-1", "topic-1", 100, 100, false, 4, 2, Instant.now()))
                .when(new ProgressionCommand.AwardXpCommand(ProgressionRules.progressIdFor("user-1"), "user-1", "game-1", "topic-1", 100, true, 4, 2))
                .expectNoEvents();
    }

    @Test
    void alreadyEarnedFirstWin_isNotReapplied() {
        fixture
                .given(new ProgressionEvent.XpAwardedEvent(
                                "user-1", "game-1", "topic-1", 100, 100, true, 4, 2, Instant.now()),
                        new ProgressionEvent.BadgeEarnedEvent("user-1", Badge.FIRST_WIN, Instant.now()))
                .when(new ProgressionCommand.AwardXpCommand(ProgressionRules.progressIdFor("user-1"), "user-1", "game-2", "topic-1", 30, true, 4, 2))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        ProgressionEvent.XpAwardedEvent.class,
                        e -> "game-2".equals(((ProgressionEvent.XpAwardedEvent) e).gameId())));
    }
}
