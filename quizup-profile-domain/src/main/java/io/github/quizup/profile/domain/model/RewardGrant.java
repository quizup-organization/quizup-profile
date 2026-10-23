package io.github.quizup.profile.domain.model;

import java.time.Instant;

/** Récompense débloquée par un joueur, avec son origine et sa date de déblocage. */
public record RewardGrant(
        RewardCode code,
        RewardSource source,
        Instant unlockedAt
) {
}
