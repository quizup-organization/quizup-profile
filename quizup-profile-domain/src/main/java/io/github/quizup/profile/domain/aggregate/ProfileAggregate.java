package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.exception.ProfileProblems;
import io.github.quizup.profile.domain.model.*;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
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

    private int level;
    private int experience;

    private int wins;
    private int losses;
    private int draws;

    private int winStreak;
    private int lossStreak;
    private int drawStreak;

    private Set<Badge> badges;
    private Deque<ProfileGame> games;
    private Map<String, ProfileTopicAggregate> topics;

    private Instant createdAt;
    private Instant updatedAt;

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

    @CommandHandler
    public void handle(ProfileCommand.AddGameResultCommand command) {
        if (StringUtils.isBlank(command.gameId())) {
            throw new ProfileProblems.MissingGameIdProblem(command.profileId());
        }

        if (StringUtils.isBlank(command.topicId())) {
            throw new ProfileProblems.MissingTopicIdProblem(command.profileId());
        }

        if (command.result() == null) {
            throw new ProfileProblems.MissingGameResultProblem(command.profileId());
        }

        if (command.playerScore() < 0) {
            throw new ProfileProblems.InvalidGameScoreProblem(command.profileId(), command.playerScore());
        }

        if (command.opponentScore() < 0) {
            throw new ProfileProblems.InvalidGameScoreProblem(command.profileId(), command.opponentScore());
        }

        ProfileGame game = ProfileGame
                .builder()
                .gameId(command.gameId())
                .topicId(command.topicId())
                .opponentId(command.opponentId())
                .opponentName(command.opponentName())
                .playerScore(command.playerScore())
                .opponentScore(command.opponentScore())
                .result(command.result())
                .playedAt(command.playedAt())
                .build();

        apply(
                new ProfileEvent.GameResultAddedEvent(
                        profileId,
                        game,
                        Instant.now()
                ));
    }

    @EventSourcingHandler
    public void on(ProfileEvent.ProfileCreatedEvent event) {
        this.profileId = event.profileId();
        this.winStreak = 0;
        this.lossStreak = 0;
        this.drawStreak = 0;
        this.experience = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.badges = new HashSet<>();
        this.topics = new HashMap<>();
        this.games = new ArrayDeque<>(MAX_RECENT_GAMES);
        this.createdAt = event.createdAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.GameResultAddedEvent event) {
        ProfileGame currentGame = event.game();

        experience += ProfileRules.computeXpEarned(currentGame.playerScore(), currentGame.result());

        switch (currentGame.result()) {
            case WIN -> wins++;
            case LOSS -> losses++;
            case DRAW -> draws++;
        }

        ProfileGame previousGame = this.games.peekLast();

        Streak streak = ProfileRules.computeStreak(
                ProfileStreak.of(winStreak, lossStreak, drawStreak),
                currentGame,
                previousGame
        );

        this.winStreak = streak.winStreak();
        this.lossStreak = streak.lossStreak();
        this.drawStreak = streak.drawStreak();

        if (this.games.size() >= MAX_RECENT_GAMES) {
            this.games.pollFirst();
        }

        this.games.addLast(currentGame);

        ProfileTopicAggregate topic = topics.getOrDefault(currentGame.topicId(), new ProfileTopicAggregate(currentGame.topicId()));
        topic.addGame(currentGame, event.recordedAt());
        topics.put(event.game().topicId(), topic);

        updatedAt = event.recordedAt();
    }
}

