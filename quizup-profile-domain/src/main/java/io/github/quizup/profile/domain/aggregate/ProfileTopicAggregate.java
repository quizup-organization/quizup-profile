package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.model.ProfileGame;
import io.github.quizup.profile.domain.model.ProfileRules;
import io.github.quizup.profile.domain.model.ProfileStreak;
import io.github.quizup.profile.domain.model.Streak;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

import static io.github.quizup.profile.domain.model.ProfileRules.MAX_RECENT_GAMES;

@Getter
public class ProfileTopicAggregate {

    private final String topicId;

    private int experience;
    private int level;

    private int wins;
    private int losses;
    private int draws;

    private int winStreak;
    private int lossStreak;
    private int drawStreak;

    private final Deque<ProfileGame> games;

    private final Instant createdAt;
    private Instant updatedAt;

    public ProfileTopicAggregate(String topicId) {
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
        this.createdAt = Instant.now();
    }

    public void addGame(ProfileGame game, Instant recordedAt) {
        experience += ProfileRules.computeXpEarned(game.playerScore(), game.result());

        switch (game.result()) {
            case WIN -> wins++;
            case LOSS -> losses++;
            case DRAW -> draws++;
        }

        if (this.games.size() >= MAX_RECENT_GAMES) {
            this.games.pollFirst();
        }

        this.games.addLast(game);

        Streak streak = ProfileRules.computeStreak(
                new ProfileStreak(winStreak, lossStreak, drawStreak),
                game,
                games.peekLast()
        );

        this.winStreak = streak.winStreak();
        this.lossStreak = streak.lossStreak();
        this.drawStreak = streak.drawStreak();

        updatedAt = recordedAt;
    }
}
