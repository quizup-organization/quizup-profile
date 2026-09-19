package io.github.quizup.profile.domain.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public interface ProfileCommand {

    String userId();

    record CreateProfileCommand(
            @TargetAggregateIdentifier String userId,
            String email,
            String initialDisplayName
    ) implements ProfileCommand {
    }

    record UpdateProfileCommand(
            @TargetAggregateIdentifier String userId,
            String requestedBy,
            String displayName,
            String bio,
            String country
    ) implements ProfileCommand {
    }
}
