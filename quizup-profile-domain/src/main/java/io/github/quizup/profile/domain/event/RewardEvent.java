package io.github.quizup.profile.domain.event;

import io.github.quizup.profile.domain.model.RewardCode;
import io.github.quizup.profile.domain.model.RewardSource;

import java.time.Instant;

/** Événements de l'agrégat des récompenses. */
public interface RewardEvent {

    String userId();

    record RewardGrantedEvent(
            String userId,
            RewardCode rewardCode,
            RewardSource source,
            Instant unlockedAt
    ) implements RewardEvent {
    }
}
