package io.github.quizup.profile.infrastructure.in.api.response;

import io.github.quizup.profile.domain.model.GameResult;

import java.io.Serializable;
import java.time.Instant;

public record GameResultResponse(
        String gameId,
        String topicId,
        String opponentId,
        int playerScore,
        int opponentScore,
        GameResult result,
        Instant playedAt
) implements Serializable {
}

