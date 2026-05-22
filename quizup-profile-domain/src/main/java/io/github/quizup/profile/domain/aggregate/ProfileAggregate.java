package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.model.*;
import lombok.Getter;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.Instant;
import java.util.*;

import static io.github.quizup.profile.domain.model.ProfileRules.MAX_RECENT_GAMES;
import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Getter
@Aggregate
public class ProfileAggregate {

    @AggregateIdentifier
    private String profileId;

    private int winStreak;
    private int lossStreak;
    private int drawStreak;

    private Map<BadgeType, Badge> badges;

    private GlobalStatistics globalStatistics;

    private Map<String, TopicStatistics> topicStatistics;
    private Deque<GameResult> recentGameResults;

    private Instant createdAt;

    protected ProfileAggregate() {
    }

    @CommandHandler
    public ProfileAggregate(ProfileCommand.CreateProfileCommand command) {
        apply(
                new ProfileEvent.ProfileCreatedEvent(
                        command.profileId(),
                        Instant.now()
                )
        );
    }

    @EventSourcingHandler
    public void on(ProfileEvent.ProfileCreatedEvent event) {
        this.profileId = event.profileId();
        this.globalStatistics = GlobalStatistics.empty();
        this.topicStatistics = new HashMap<>();
        this.badges = new EnumMap<>(BadgeType.class);
        this.recentGameResults = new ArrayDeque<>(MAX_RECENT_GAMES);
        this.createdAt = event.createdAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.GameResultRecordedEvent event) {
        if (recentGameResults.size() >= MAX_RECENT_GAMES) {
            recentGameResults.pollFirst(); // retire le plus ancien
        }
        recentGameResults.addLast(event.gameResult()); // ajoute le plus récent
    }

    public List<GameResult> getRecentGameResults() {
        return List.copyOf(recentGameResults);
    }
}

