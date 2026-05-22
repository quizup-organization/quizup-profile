package io.github.quizup.profile.domain.model;

import lombok.Builder;

import java.time.Instant;

@Builder(toBuilder = true)
public record GameResult(
        String gameId,
        String topicId,
        String topicName,
        String opponentId,
        String opponentName,
        int playerScore,
        int opponentScore,
        GameResultType result,
        Instant playedAt
) {
}
