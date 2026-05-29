package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.event.ProfileTopicEvent;
import io.github.quizup.profile.domain.model.ProfileGame;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

import static io.github.quizup.profile.domain.model.ProfileRules.MAX_RECENT_GAMES;

@Getter
public class ProfileTopicAggregate {

    private String topicId;

    private int level;
    private int experience;

    private int wins;
    private int losses;
    private int draws;

    private int winStreak;
    private int lossStreak;
    private int drawStreak;

    private Deque<ProfileGame> games;

    private Instant createdAt;
    private Instant updatedAt;

    protected ProfileTopicAggregate() {
    }

    public ProfileTopicAggregate(String topicId, Instant createdAt) {
        this.topicId = topicId;
        this.experience = 0;
        this.level = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.winStreak = 0;
        this.lossStreak = 0;
        this.drawStreak = 0;
        this.games = new ArrayDeque<>();
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    // ── EventSourcingHandlers ───────────────────────────────────────────────

    public void on(ProfileTopicEvent.ProfileTopicCreatedEvent event) {
        this.topicId = event.topicId();
        this.experience = 0;
        this.level = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.winStreak = 0;
        this.lossStreak = 0;
        this.drawStreak = 0;
        this.games = new ArrayDeque<>();
        this.createdAt = event.createdAt();
        this.updatedAt = event.createdAt();
    }

    public void on(ProfileTopicEvent.TopicExperienceIncreasedEvent event) {
        this.experience += event.experienceEarned();
        this.updatedAt = event.earnedAt();
    }

    public void on(ProfileTopicEvent.TopicLevelIncreasedEvent event) {
        this.level = event.level();
        this.updatedAt = event.increasedAt();
    }

    public void on(ProfileTopicEvent.TopicWinsIncreasedEvent event) {
        this.wins = event.wins();
        this.updatedAt = event.increasedAt();
    }

    public void on(ProfileTopicEvent.TopicLossesIncreasedEvent event) {
        this.losses = event.losses();
        this.updatedAt = event.increasedAt();
    }

    public void on(ProfileTopicEvent.TopicDrawsIncreasedEvent event) {
        this.draws = event.draws();
        this.updatedAt = event.increasedAt();
    }

    public void on(ProfileTopicEvent.TopicWinStreakIncreasedEvent event) {
        this.winStreak = event.winStreak();
        this.lossStreak = 0;
        this.drawStreak = 0;
        this.updatedAt = event.increasedAt();
    }

    public void on(ProfileTopicEvent.TopicWinStreakEndedEvent event) {
        this.winStreak = 0;
        this.updatedAt = event.endedAt();
    }

    public void on(ProfileTopicEvent.TopicLossStreakIncreasedEvent event) {
        this.lossStreak = event.lossStreak();
        this.winStreak = 0;
        this.drawStreak = 0;
        this.updatedAt = event.increasedAt();
    }

    public void on(ProfileTopicEvent.TopicLossStreakEndedEvent event) {
        this.lossStreak = 0;
        this.updatedAt = event.endedAt();
    }

    public void on(ProfileTopicEvent.TopicDrawStreakIncreasedEvent event) {
        this.drawStreak = event.drawStreak();
        this.winStreak = 0;
        this.lossStreak = 0;
        this.updatedAt = event.increasedAt();
    }

    public void on(ProfileTopicEvent.TopicDrawStreakEndedEvent event) {
        this.drawStreak = 0;
        this.updatedAt = event.endedAt();
    }

    public void on(ProfileTopicEvent.TopicGamePlayedEvent event) {
        if (this.games == null) {
            this.games = new ArrayDeque<>();
        }
        if (this.games.size() >= MAX_RECENT_GAMES) {
            this.games.pollFirst();
        }
        this.games.addLast(event.game());
        this.updatedAt = event.playedAt();
    }
}
