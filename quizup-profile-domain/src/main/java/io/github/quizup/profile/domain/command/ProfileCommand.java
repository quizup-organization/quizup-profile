package io.github.quizup.profile.domain.command;

import io.github.quizup.profile.domain.model.GameResultType;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public interface ProfileCommand {
    String profileId();

    record CreateProfileCommand(
            @TargetAggregateIdentifier String profileId
    ) implements ProfileCommand {
    }

    record AddGameResultCommand(
            @TargetAggregateIdentifier String profileId,
            String gameId,
            String topicId,
            String opponentId,
            String opponentName,
            int playerScore,
            int opponentScore,
            GameResultType result
    ) implements ProfileCommand {
    }
}

