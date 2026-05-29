package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.event.ProfileTopicEvent;
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

    // ── Command Handlers ────────────────────────────────────────────────────

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

        ProfileGame game = ProfileGame.builder()
                .gameId(command.gameId())
                .topicId(command.topicId())
                .opponentId(command.opponentId())
                .opponentName(command.opponentName())
                .playerScore(command.playerScore())
                .opponentScore(command.opponentScore())
                .result(command.result())
                .playedAt(command.playedAt())
                .build();

        Instant now = Instant.now();
        String topicId = command.topicId();
        GameResult result = command.result();

        int experienceEarned = ProfileRules.computeXpEarned(command.playerScore(), result);

        // ── Topic events ────────────────────────────────────────────────────

        boolean isNewTopic = !topics.containsKey(topicId);

        if (isNewTopic) {
            apply(
                    new ProfileTopicEvent.ProfileTopicCreatedEvent(
                            profileId,
                            topicId,
                            now
                    )
            );
        }

        ProfileTopicAggregate topicAggregate = isNewTopic
                ? new ProfileTopicAggregate(topicId, now)
                : topics.get(topicId);

        apply(
                new ProfileTopicEvent.TopicExperienceIncreasedEvent(
                        profileId,
                        topicId,
                        experienceEarned,
                        now
                )
        );

        int topicExperience = topicAggregate.getExperience() + experienceEarned;
        int topicLevel = ProfileRules.computeLevelFromXp(topicExperience);

        if (topicLevel > topicAggregate.getLevel()) {
            apply(
                    new ProfileTopicEvent.TopicLevelIncreasedEvent(
                            profileId,
                            topicId,
                            topicLevel,
                            now
                    )
            );
        }

        switch (result) {
            case WIN -> apply(
                    new ProfileTopicEvent.TopicWinsIncreasedEvent(
                            profileId,
                            topicId,
                            topicAggregate.getWins() + 1,
                            now
                    )
            );
            case LOSS -> apply(
                    new ProfileTopicEvent.TopicLossesIncreasedEvent(
                            profileId,
                            topicId,
                            topicAggregate.getLosses() + 1,
                            now
                    )
            );
            case DRAW -> apply(
                    new ProfileTopicEvent.TopicDrawsIncreasedEvent(
                            profileId,
                            topicId,
                            topicAggregate.getDraws() + 1,
                            now
                    )
            );
        }

        ProfileGame topicPreviousGame = (topicAggregate.getGames() != null && !topicAggregate.getGames().isEmpty())
                ? topicAggregate.getGames().peekLast()
                : null;
        Streak topicStreak = ProfileRules.computeStreak(
                ProfileStreak.of(topicAggregate.getWinStreak(), topicAggregate.getLossStreak(), topicAggregate.getDrawStreak()),
                game,
                topicPreviousGame
        );
        applyTopicStreakEvents(profileId, topicId, result, topicAggregate, topicStreak, now);

        apply(new ProfileTopicEvent.TopicGamePlayedEvent(profileId, topicId, game, now));

        // ── Global events ───────────────────────────────────────────────────

        apply(new ProfileEvent.ExperienceIncreasedEvent(profileId, experienceEarned, now));

        int newGlobalXp = experience + experienceEarned;
        int newGlobalLevel = ProfileRules.computeLevelFromXp(newGlobalXp);
        if (newGlobalLevel > level) {
            apply(new ProfileEvent.LevelIncreasedEvent(profileId, newGlobalLevel, now));
        }

        switch (result) {
            case WIN -> apply(
                    new ProfileEvent.WinsIncreasedEvent(
                            profileId,
                            wins + 1,
                            now
                    )
            );
            case LOSS -> apply(new ProfileEvent.LossesIncreasedEvent(profileId, losses + 1, now));
            case DRAW -> apply(new ProfileEvent.DrawsIncreasedEvent(profileId, draws + 1, now));
        }

        ProfileGame globalPreviousGame = (games != null && !games.isEmpty()) ? games.peekLast() : null;
        Streak globalStreak = ProfileRules.computeStreak(
                ProfileStreak.of(winStreak, lossStreak, drawStreak),
                game,
                globalPreviousGame
        );
        applyGlobalStreakEvents(profileId, result, globalStreak, now);

        apply(new ProfileEvent.GamePlayedEvent(profileId, game, now));
    }

    // ── Event Sourcing Handlers ─────────────────────────────────────────────

    @EventSourcingHandler
    public void on(ProfileEvent.ProfileCreatedEvent event) {
        this.profileId = event.profileId();
        this.experience = 0;
        this.level = 0;
        this.winStreak = 0;
        this.lossStreak = 0;
        this.drawStreak = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.badges = new HashSet<>();
        this.topics = new HashMap<>();
        this.games = new ArrayDeque<>(MAX_RECENT_GAMES);
        this.createdAt = event.createdAt();
        this.updatedAt = event.createdAt();
    }

    @EventSourcingHandler
    public void on(ProfileTopicEvent.ProfileTopicCreatedEvent event) {
        topics.put(event.topicId(), new ProfileTopicAggregate(event.topicId(), event.createdAt()));
    }

    @EventSourcingHandler
    public void on(ProfileTopicEvent event) {
        ProfileTopicAggregate topic = topics.get(event.topicId());
        if (topic == null) return;
        switch (event) {
            case ProfileTopicEvent.TopicExperienceIncreasedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicLevelIncreasedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicWinsIncreasedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicLossesIncreasedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicDrawsIncreasedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicWinStreakIncreasedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicWinStreakEndedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicLossStreakIncreasedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicLossStreakEndedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicDrawStreakIncreasedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicDrawStreakEndedEvent e -> topic.on(e);
            case ProfileTopicEvent.TopicGamePlayedEvent e -> topic.on(e);
            default -> { /* ProfileTopicCreatedEvent traité séparément */ }
        }
    }

    @EventSourcingHandler
    public void on(ProfileEvent.ExperienceIncreasedEvent event) {
        this.experience += event.experienceEarned();
        this.updatedAt = event.earnedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.LevelIncreasedEvent event) {
        this.level = event.level();
        this.updatedAt = event.increasedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.WinsIncreasedEvent event) {
        this.wins = event.wins();
        this.updatedAt = event.increasedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.LossesIncreasedEvent event) {
        this.losses = event.losses();
        this.updatedAt = event.increasedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.DrawsIncreasedEvent event) {
        this.draws = event.draws();
        this.updatedAt = event.increasedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.WinStreakIncreasedEvent event) {
        this.winStreak = event.winStreak();
        this.lossStreak = 0;
        this.drawStreak = 0;
        this.updatedAt = event.increasedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.WinStreakEndedEvent event) {
        this.winStreak = 0;
        this.updatedAt = event.endedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.LossStreakIncreasedEvent event) {
        this.lossStreak = event.lossStreak();
        this.winStreak = 0;
        this.drawStreak = 0;
        this.updatedAt = event.increasedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.LossStreakEndedEvent event) {
        this.lossStreak = 0;
        this.updatedAt = event.endedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.DrawStreakIncreasedEvent event) {
        this.drawStreak = event.drawStreak();
        this.winStreak = 0;
        this.lossStreak = 0;
        this.updatedAt = event.increasedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.DrawStreakEndedEvent event) {
        this.drawStreak = 0;
        this.updatedAt = event.endedAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.GamePlayedEvent event) {
        if (this.games.size() >= MAX_RECENT_GAMES) {
            this.games.pollFirst();
        }
        this.games.addLast(event.game());
        this.updatedAt = event.playedAt();
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private void applyTopicStreakEvents(String profileId,
                                        String topicId,
                                        GameResult result,
                                        ProfileTopicAggregate topic,
                                        Streak newStreak,
                                        Instant now) {
        switch (result) {
            case WIN -> {
                if (topic.getLossStreak() > 0)
                    apply(new ProfileTopicEvent.TopicLossStreakEndedEvent(profileId, topicId, now));
                if (topic.getDrawStreak() > 0)
                    apply(new ProfileTopicEvent.TopicDrawStreakEndedEvent(profileId, topicId, now));
                apply(new ProfileTopicEvent.TopicWinStreakIncreasedEvent(profileId, topicId, newStreak.winStreak(), now));
            }
            case LOSS -> {
                if (topic.getWinStreak() > 0)
                    apply(new ProfileTopicEvent.TopicWinStreakEndedEvent(profileId, topicId, now));
                if (topic.getDrawStreak() > 0)
                    apply(new ProfileTopicEvent.TopicDrawStreakEndedEvent(profileId, topicId, now));
                apply(new ProfileTopicEvent.TopicLossStreakIncreasedEvent(profileId, topicId, newStreak.lossStreak(), now));
            }
            case DRAW -> {
                if (topic.getWinStreak() > 0)
                    apply(new ProfileTopicEvent.TopicWinStreakEndedEvent(profileId, topicId, now));
                if (topic.getLossStreak() > 0)
                    apply(new ProfileTopicEvent.TopicLossStreakEndedEvent(profileId, topicId, now));
                apply(new ProfileTopicEvent.TopicDrawStreakIncreasedEvent(profileId, topicId, newStreak.drawStreak(), now));
            }
        }
    }

    private void applyGlobalStreakEvents(String profileId, GameResult result, Streak newStreak, Instant now) {
        switch (result) {
            case WIN -> {
                if (lossStreak > 0) apply(new ProfileEvent.LossStreakEndedEvent(profileId, now));
                if (drawStreak > 0) apply(new ProfileEvent.DrawStreakEndedEvent(profileId, now));
                apply(new ProfileEvent.WinStreakIncreasedEvent(profileId, newStreak.winStreak(), now));
            }
            case LOSS -> {
                if (winStreak > 0) apply(new ProfileEvent.WinStreakEndedEvent(profileId, now));
                if (drawStreak > 0) apply(new ProfileEvent.DrawStreakEndedEvent(profileId, now));
                apply(new ProfileEvent.LossStreakIncreasedEvent(profileId, newStreak.lossStreak(), now));
            }
            case DRAW -> {
                if (winStreak > 0) apply(new ProfileEvent.WinStreakEndedEvent(profileId, now));
                if (lossStreak > 0) apply(new ProfileEvent.LossStreakEndedEvent(profileId, now));
                apply(new ProfileEvent.DrawStreakIncreasedEvent(profileId, newStreak.drawStreak(), now));
            }
        }
    }
}

