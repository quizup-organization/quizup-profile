package io.github.quizup.profile.domain.command;

import io.github.quizup.profile.domain.model.ProfileMatchResult;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public interface ProfileCommand {
    String profileId();

    record CreateProfileCommand(
            @TargetAggregateIdentifier String profileId,
            String userId
    ) implements ProfileCommand {
    }

    record RecordProfileMatchResultCommand(
            @TargetAggregateIdentifier String profileId,
            String topicId,
            ProfileMatchResult result
    ) implements ProfileCommand {
    }
}

