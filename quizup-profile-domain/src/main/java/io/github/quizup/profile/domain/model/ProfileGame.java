package io.github.quizup.profile.domain.model;

import lombok.Builder;

import java.time.Instant;

@Builder(toBuilder = true)
public record ProfileGame(
        String gameId,
        String topicId,
        String opponentId,
        String opponentName,
        int playerScore,
        int opponentScore,
        GameResult result,
        Instant playedAt
) {
}
