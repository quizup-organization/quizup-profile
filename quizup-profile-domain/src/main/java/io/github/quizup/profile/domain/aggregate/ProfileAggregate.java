package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.exception.ProfileProblems;
import io.github.quizup.profile.domain.model.ProfileMatchResult;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
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
    private String pseudonym;

    private int gamesPlayed;
    private int wins;
    private int losses;
    private int draws;
    private double winLossRatio;

    private final Map<String, ProfileTopicProgression> topicProgressions = new HashMap<>();

    protected ProfileAggregate() {
    }

    @CommandHandler
    public ProfileAggregate(ProfileCommand.CreateProfileCommand command) {
        if (StringUtils.isBlank(command.userId())) {
            throw new ProfileProblems.MissingUserIdProblem(command.profileId());
        }

        apply(new ProfileEvent.ProfileCreatedEvent(
                command.profileId(),
                command.userId(),
                Instant.now()
        ));
    }

    @CommandHandler
    public void handle(ProfileCommand.RecordProfileMatchResultCommand command) {
        if (StringUtils.isBlank(command.topicId())) {
            throw new ProfileProblems.MissingTopicIdProblem(profileId);
        }
        if (command.result() == null) {
            throw new ProfileProblems.MissingMatchResultProblem(profileId);
        }

        ProfileStatsSnapshot nextStats = computeNextGlobalStats(command.result());

        apply(new ProfileEvent.ProfileMatchResultRecordedEvent(
                profileId,
                command.topicId(),
                command.result(),
                nextStats.gamesPlayed(),
                nextStats.wins(),
                nextStats.losses(),
                nextStats.draws(),
                nextStats.winLossRatio(),
                Instant.now()
        ));
    }

    @EventSourcingHandler
    public void on(ProfileEvent.ProfileCreatedEvent event) {
        this.profileId = event.profileId();
        this.userId = event.userId();
        this.gamesPlayed = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.winLossRatio = 0.0d;
    }

    @EventSourcingHandler
    public void on(ProfileEvent.ProfileMatchResultRecordedEvent event) {
        this.gamesPlayed = event.totalGamesPlayed();
        this.wins = event.totalWins();
        this.losses = event.totalLosses();
        this.draws = event.totalDraws();
        this.winLossRatio = event.winLossRatio();

        ProfileTopicProgression progression = topicProgressions.computeIfAbsent(
                event.topicId(),
                ProfileTopicProgression::new
        );
        progression.record(event.result());
    }

    private ProfileStatsSnapshot computeNextGlobalStats(ProfileMatchResult result) {
        int nextGamesPlayed = gamesPlayed + 1;
        int nextWins = wins;
        int nextLosses = losses;
        int nextDraws = draws;

        switch (result) {
            case WIN -> nextWins++;
            case LOSS -> nextLosses++;
            case DRAW -> nextDraws++;
        }

        double nextRatio = calculateWinLossRatio(nextWins, nextLosses);
        return new ProfileStatsSnapshot(nextGamesPlayed, nextWins, nextLosses, nextDraws, nextRatio);
    }

    private static double calculateWinLossRatio(int wins, int losses) {
        if (losses == 0) {
            return wins;
        }
        return (double) wins / losses;
    }

    private record ProfileStatsSnapshot(
            int gamesPlayed,
            int wins,
            int losses,
            int draws,
            double winLossRatio
    ) {
    }
}

