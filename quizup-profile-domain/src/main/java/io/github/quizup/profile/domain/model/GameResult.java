package io.github.quizup.profile.domain.model;

import lombok.Builder;

import java.time.Instant;

@Builder(toBuilder = true)
public record GameResult(
        String gameId,
        String topicId,
        String opponentId,
        int playerScore,
        int opponentScore,
        GameResultType result,
        Instant playedAt
) {
}
