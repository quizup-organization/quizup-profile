package io.github.quizup.profile.infrastructure.in.api.response;

import io.github.quizup.profile.domain.model.GameResultType;

import java.io.Serializable;
import java.time.Instant;

public record GameResultResponse(
        String gameId,
        String topicId,
        String topicName,
        String opponentId,
        String opponentName,
        int playerScore,
        int opponentScore,
        GameResultType result,
        Instant playedAt
) implements Serializable {
}

