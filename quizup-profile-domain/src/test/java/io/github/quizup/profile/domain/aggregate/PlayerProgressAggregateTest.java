package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import io.github.quizup.profile.domain.command.ProgressionCommand;
import io.github.quizup.profile.domain.event.ProgressionEvent;
import io.github.quizup.profile.domain.model.ProgressionRules;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * Test Axon in-memory de {@link PlayerProgressAggregate} : attribution XP, idempotence et montée
 * de niveau. Les **badges** sont évalués par {@link BadgeAggregate} (cf. {@code BadgeAggregateTest}).
 */
class PlayerProgressAggregateTest {

    private final AggregateTestFixture<PlayerProgressAggregate> fixture =
            new AggregateTestFixture<>(PlayerProgressAggregate.class);

    @Test
    void firstAward_createsAggregateAndAppliesXpAwardedEvent() {
        fixture
                .givenNoPriorActivity()
                .when(new ProgressionCommand.AwardXpCommand(ProgressionRules.progressIdFor("user-1"), "user-1", "game-1", "topic-1", 100, false, false, false, 4, 2, 0))
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
    void botGame_isTrainingAndAwardsNoXpEvenOnWin() {
        fixture
                .givenNoPriorActivity()
                .when(new ProgressionCommand.AwardXpCommand(ProgressionRules.progressIdFor("user-1"), "user-1", "game-1", "topic-1", 160, true, false, true, 4, 2, 0))
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        ProgressionEvent.XpAwardedEvent.class,
                        e -> {
                            ProgressionEvent.XpAwardedEvent xp = (ProgressionEvent.XpAwardedEvent) e;
                            return xp.botGame() && xp.xp() == 0;
                        }));
    }

    @Test
    void crossingThreshold_appliesLevelReachedEvent() {
        fixture
                .givenNoPriorActivity()
                .when(new ProgressionCommand.AwardXpCommand(ProgressionRules.progressIdFor("user-1"), "user-1", "game-1", "topic-1", 100, true, false, false, 4, 2, 0))
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        ProgressionEvent.LevelReachedEvent.class,
                        e -> ((ProgressionEvent.LevelReachedEvent) e).level() == 2));
    }

    @Test
    void sameGameId_isIdempotent() {
        fixture
                .given(new ProgressionEvent.XpAwardedEvent(
                        "user-1", "game-1", "topic-1", 100, 100, false, false, false, 4, 2, 0, Instant.now()))
                .when(new ProgressionCommand.AwardXpCommand(ProgressionRules.progressIdFor("user-1"), "user-1", "game-1", "topic-1", 100, true, false, false, 4, 2, 0))
                .expectNoEvents();
    }
}
