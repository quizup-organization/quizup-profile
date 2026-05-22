package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.exception.ProfileProblems;
import io.github.quizup.profile.domain.model.Statistics;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateMember;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Getter
@Aggregate
public class ProfileAggregate {

    @AggregateIdentifier
    private String profileId;

    private String userId;

    private Statistics statistics;

    @AggregateMember
    private Map<String, ProfileTopicStatistics> topicStatistics;

    private Instant createdAt;

    protected ProfileAggregate() {
    }

    @CommandHandler
    public ProfileAggregate(ProfileCommand.CreateProfileCommand command) {
        if (StringUtils.isBlank(command.userId())) {
            throw new ProfileProblems.MissingUserIdProblem(command.profileId());
        }

        apply(
                new ProfileEvent.ProfileCreatedEvent(
                        command.profileId(),
                        command.userId(),
                        Instant.now()
                )
        );
    }


    @EventSourcingHandler
    public void on(ProfileEvent.ProfileCreatedEvent event) {
        this.profileId = event.profileId();
        this.userId = event.userId();
        this.createdAt = event.createdAt();
        this.topicStatistics = new HashMap<>();
    }

}

