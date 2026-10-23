package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import io.github.quizup.profile.domain.command.RewardCommand;
import io.github.quizup.profile.domain.event.RewardEvent;
import io.github.quizup.profile.domain.model.RewardCode;
import io.github.quizup.profile.domain.model.RewardRules;
import io.github.quizup.profile.domain.model.RewardSource;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class PlayerRewardAggregateTest {

    private final AggregateTestFixture<PlayerRewardAggregate> fixture =
            new AggregateTestFixture<>(PlayerRewardAggregate.class);

    @Test
    void firstGrant_emitsRewardGranted() {
        Instant at = Instant.parse("2026-10-23T10:00:00Z");
        fixture.givenNoPriorActivity()
                .when(new RewardCommand.GrantRewardCommand(
                        RewardRules.rewardIdFor("user-1"), "user-1",
                        RewardCode.MICAH_AVATAR, RewardSource.BADGE, at))
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        RewardEvent.RewardGrantedEvent.class,
                        e -> ((RewardEvent.RewardGrantedEvent) e).rewardCode() == RewardCode.MICAH_AVATAR
                                && "user-1".equals(((RewardEvent.RewardGrantedEvent) e).userId())));
    }

    @Test
    void duplicateGrant_isIdempotent() {
        Instant at = Instant.parse("2026-10-23T10:00:00Z");
        fixture.given(new RewardEvent.RewardGrantedEvent(
                        "user-1", RewardCode.MICAH_AVATAR, RewardSource.BADGE, at))
                .when(new RewardCommand.GrantRewardCommand(
                        RewardRules.rewardIdFor("user-1"), "user-1",
                        RewardCode.MICAH_AVATAR, RewardSource.BADGE, at))
                .expectNoEvents();
    }
}
