package io.github.quizup.profile.domain.command;

import io.github.quizup.profile.domain.model.GameResult;
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
            GameResult gameResult
    ) implements ProfileCommand {
    }
}

