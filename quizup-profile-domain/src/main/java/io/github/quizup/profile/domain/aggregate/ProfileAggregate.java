package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.command.ProfileCommand;
import io.github.quizup.profile.domain.event.ProfileEvent;
import io.github.quizup.profile.domain.exception.ProfileProblems;
import io.github.quizup.profile.domain.model.*;
import lombok.Getter;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.Instant;
import java.util.*;

import static io.github.quizup.profile.domain.model.ProfileRules.MAX_RECENT_GAMES;
import static io.github.quizup.profile.domain.model.ProfileRules.*;
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
    private Instant updatedAt;

    protected ProfileAggregate() {
    }

    @CommandHandler
    public ProfileAggregate(ProfileCommand.CreateProfileCommand command) {
        if (command.profileId() == null || command.profileId().isBlank()) {
            throw new ProfileProblems.MissingProfileIdProblem(command.profileId());
        }

        Instant now = Instant.now();
        Profile profile = Profile.empty(command.profileId(), now);

        apply(
                new ProfileEvent.ProfileCreatedEvent(
                        command.profileId(),
                        profile,
                        now
                )
        );
    }

    @CommandHandler
    public void handle(ProfileCommand.AddGameResultCommand command) {
        if (command.profileId() == null || command.profileId().isBlank()) {
            throw new ProfileProblems.MissingProfileIdProblem(command.profileId());
        }

        if (command.gameResult() == null) {
            throw new ProfileProblems.MissingGameResultProblem(command.profileId());
        }

        if (command.gameResult().gameId() == null || command.gameResult().gameId().isBlank()) {
            throw new ProfileProblems.MissingGameIdProblem(command.profileId());
        }

        if (command.gameResult().topicId() == null || command.gameResult().topicId().isBlank()) {
            throw new ProfileProblems.MissingTopicIdProblem(command.profileId());
        }

        if (command.gameResult().playerScore() < 0) {
            throw new ProfileProblems.InvalidGameScoreProblem(command.profileId(), command.gameResult().playerScore());
        }

        int xpEarned = ProfileRules.computeXpEarned(command.gameResult().playerScore(), command.gameResult().result());

        GlobalStatistics newGlobal = globalStatistics.addGame(xpEarned, command.gameResult().result());

        Map<String, TopicStatistics> newTopicStatistics = new HashMap<>(topicStatistics);
        TopicStatistics currentTopicStats = newTopicStatistics.getOrDefault(
                command.gameResult().topicId(),
                TopicStatistics.empty(command.gameResult().topicId())
        );
        TopicStatistics updatedTopicStats = currentTopicStats.addGame(xpEarned, command.gameResult().result());
        newTopicStatistics.put(command.gameResult().topicId(), updatedTopicStats);

        int newWinStreak = winStreak;
        int newLossStreak = lossStreak;
        int newDrawStreak = drawStreak;

        switch (command.gameResult().result()) {
            case WIN -> {
                newWinStreak++;
                newLossStreak = 0;
                newDrawStreak = 0;
            }
            case LOSS -> {
                newLossStreak++;
                newWinStreak = 0;
                newDrawStreak = 0;
            }
            case DRAW -> {
                newDrawStreak++;
                newWinStreak = 0;
                newLossStreak = 0;
            }
        }

        Map<BadgeType, Badge> newBadges = new EnumMap<>(BadgeType.class);
        newBadges.putAll(badges);
        unlockNewBadges(newBadges, command.gameResult(), newGlobal, updatedTopicStats, newWinStreak);

        List<GameResult> newRecentResults = new ArrayList<>(recentGameResults);

        if (newRecentResults.size() >= MAX_RECENT_GAMES) {
            newRecentResults.remove(0);
        }

        newRecentResults.add(command.gameResult());

        Instant now = Instant.now();
        Profile profile = new Profile(
                profileId,
                newWinStreak,
                newLossStreak,
                newDrawStreak,
                newGlobal,
                Map.copyOf(newTopicStatistics),
                Map.copyOf(newBadges),
                List.copyOf(newRecentResults),
                createdAt,
                now
        );

        apply(
                new ProfileEvent.GameResultRecordedEvent(
                        profileId,
                        command.gameResult(),
                        profile,
                        now
                )
        );
    }

    @EventSourcingHandler
    public void on(ProfileEvent.ProfileCreatedEvent event) {
        applyProfile(event.profile());
    }

    @EventSourcingHandler
    public void on(ProfileEvent.GameResultRecordedEvent event) {
        applyProfile(event.profile());
    }

    private void applyProfile(Profile profile) {
        this.profileId = profile.profileId();
        this.winStreak = profile.winStreak();
        this.lossStreak = profile.lossStreak();
        this.drawStreak = profile.drawStreak();
        this.globalStatistics = profile.globalStatistics();
        this.topicStatistics = new HashMap<>(profile.topicStatistics());
        this.badges = new EnumMap<>(BadgeType.class);
        this.badges.putAll(profile.badges());
        this.recentGameResults = new ArrayDeque<>(profile.recentGameResults());
        this.createdAt = profile.createdAt();
        this.updatedAt = profile.updatedAt();
    }

    private void unlockNewBadges(Map<BadgeType, Badge> unlockedBadges,
                                 GameResult gameResult,
                                 GlobalStatistics globalStats,
                                 TopicStatistics topicStats,
                                 int currentWinStreak) {
        Instant unlockedAt = gameResult.playedAt() != null ? gameResult.playedAt() : Instant.now();

        if (gameResult.result() == GameResultType.WIN) {
            unlock(unlockedBadges, BadgeType.FIRST_WIN, unlockedAt);
        }
        if (gameResult.playerScore() >= PERFECT_GAME_SCORE) {
            unlock(unlockedBadges, BadgeType.PERFECT_SCORE, unlockedAt);
        }
        if (currentWinStreak >= FIRE_STREAK_5_THRESHOLD) {
            unlock(unlockedBadges, BadgeType.FIRE_STREAK_5, unlockedAt);
        }
        if (currentWinStreak >= FIRE_STREAK_10_THRESHOLD) {
            unlock(unlockedBadges, BadgeType.FIRE_STREAK_10, unlockedAt);
        }
        if (globalStats.totalGames() >= VETERAN_100_THRESHOLD) {
            unlock(unlockedBadges, BadgeType.VETERAN_100, unlockedAt);
        }
        if (topicStats.level() >= SPECIALIST_LEVEL_THRESHOLD) {
            unlock(unlockedBadges, BadgeType.SPECIALIST, unlockedAt);
        }
    }

    private void unlock(Map<BadgeType, Badge> unlockedBadges, BadgeType badgeType, Instant unlockedAt) {
        unlockedBadges.putIfAbsent(badgeType, new Badge(badgeType, unlockedAt));
    }

    public List<GameResult> getRecentGameResults() {
        return List.copyOf(recentGameResults);
    }
}

