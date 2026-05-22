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

    private int totalExperience;

    private int wins;
    private int losses;
    private int draws;

    private Map<BadgeType, Badge> badges;
    private Map<String, TopicStatistics> topicStatistics;

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

        int xpEarned = ProfileRules.computeXpEarned(command.playerScore(), command.result());

        int newGlobalXp = this.totalExperience + xpEarned;

        int newGlobalWins = command.result() == GameResultType.WIN ? wins + 1 : wins;
        int newGlobalLosses = command.result() == GameResultType.LOSS ? losses + 1 : losses;
        int newGlobalDraws = command.result() == GameResultType.DRAW ? draws + 1 : draws;

        int newWinStreak = command.result() == GameResultType.WIN ? winStreak + 1 : 0;
        int newLossStreak = command.result() == GameResultType.LOSS ? lossStreak + 1 : 0;
        int newDrawStreak = command.result() == GameResultType.DRAW ? drawStreak + 1 : 0;

        TopicStatistics currentTopicStats = topicStatistics.getOrDefault(command.topicId(), TopicStatistics.empty(command.topicId()));

        int newTopicXp = currentTopicStats.totalExperience() + xpEarned;
        int newTopicWins = command.result() == GameResultType.WIN ? currentTopicStats.wins() + 1 : currentTopicStats.wins();
        int newTopicLosses = command.result() == GameResultType.LOSS ? currentTopicStats.losses() + 1 : currentTopicStats.losses();
        int newTopicDraws = command.result() == GameResultType.DRAW ? currentTopicStats.draws() + 1 : currentTopicStats.draws();
        int newTopicWinStreak = command.result() == GameResultType.WIN ? currentTopicStats.winStreak() + 1 : 0;

        int newTotalGames = newGlobalWins + newGlobalLosses + newGlobalDraws;
        int newTopicLevel = ProfileRules.computeLevelFromXp(newTopicXp);
        Set<BadgeType> newBadges = computeNewBadges(command, newTotalGames, newWinStreak, newTopicWinStreak, newTopicLevel);

        boolean leveledUp = ProfileRules.computeLevelFromXp(newGlobalXp)
                > ProfileRules.computeLevelFromXp(this.totalExperience);

        apply(new ProfileEvent.GameResultRecordedEvent(
                profileId,
                command.gameId(),
                command.topicId(),
                command.opponentId(),
                command.playerScore(),
                command.opponentScore(),
                command.result(),
                xpEarned,
                newGlobalXp,
                newGlobalWins,
                newGlobalLosses,
                newGlobalDraws,
                newWinStreak,
                newLossStreak,
                newDrawStreak,
                newTopicXp,
                newTopicWins,
                newTopicLosses,
                newTopicDraws,
                newTopicWinStreak,
                newBadges,
                leveledUp,
                Instant.now()
        ));
    }

    @EventSourcingHandler
    public void on(ProfileEvent.ProfileCreatedEvent event) {
        this.profileId = event.profileId();
        this.winStreak = 0;
        this.lossStreak = 0;
        this.drawStreak = 0;
        this.totalExperience = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.badges = new EnumMap<>(BadgeType.class);
        this.topicStatistics = new HashMap<>();
        this.processedGameIds = new HashSet<>();
        this.createdAt = event.createdAt();
    }

    @EventSourcingHandler
    public void on(ProfileEvent.GameResultRecordedEvent event) {
        this.totalExperience = event.newGlobalTotalExperience();
        this.wins = event.newGlobalWins();
        this.losses = event.newGlobalLosses();
        this.draws = event.newGlobalDraws();
        this.winStreak = event.newWinStreak();
        this.lossStreak = event.newLossStreak();
        this.drawStreak = event.newDrawStreak();

        TopicStatistics updatedTopic = new TopicStatistics(
                event.topicId(),
                event.newTopicTotalExperience(),
                event.newTopicWins(),
                event.newTopicLosses(),
                event.newTopicDraws(),
                event.newTopicWinStreak()
        );
        this.topicStatistics.put(event.topicId(), updatedTopic);

        event.newBadges().forEach(badgeType -> this.badges.putIfAbsent(badgeType, new Badge(badgeType, event.recordedAt())));
        this.processedGameIds.add(event.gameId());
    }

    private Set<BadgeType> computeNewBadges(ProfileCommand.AddGameResultCommand command,
                                            int newTotalGames,
                                            int newWinStreak,
                                            int newTopicWinStreak,
                                            int newTopicLevel) {
        Set<BadgeType> awarded = new HashSet<>();

        if (command.result() == GameResultType.WIN && wins == 0 && !badges.containsKey(BadgeType.FIRST_WIN)) {
            awarded.add(BadgeType.FIRST_WIN);
        }
        if (command.playerScore() >= PERFECT_GAME_SCORE && !badges.containsKey(BadgeType.PERFECT_SCORE)) {
            awarded.add(BadgeType.PERFECT_SCORE);
        }
        if (newWinStreak >= FIRE_STREAK_5_THRESHOLD && !badges.containsKey(BadgeType.FIRE_STREAK_5)) {
            awarded.add(BadgeType.FIRE_STREAK_5);
        }
        if (newWinStreak >= FIRE_STREAK_10_THRESHOLD && !badges.containsKey(BadgeType.FIRE_STREAK_10)) {
            awarded.add(BadgeType.FIRE_STREAK_10);
        }
        if (newTotalGames >= VETERAN_100_THRESHOLD && !badges.containsKey(BadgeType.VETERAN_100)) {
            awarded.add(BadgeType.VETERAN_100);
        }
        if (newTopicLevel >= SPECIALIST_LEVEL_THRESHOLD && !badges.containsKey(BadgeType.SPECIALIST)) {
            awarded.add(BadgeType.SPECIALIST);
        }
        return awarded;
    }
}

