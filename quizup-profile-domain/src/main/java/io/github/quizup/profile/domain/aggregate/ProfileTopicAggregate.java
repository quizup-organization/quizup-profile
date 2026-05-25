package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.model.ProfileGame;
import io.github.quizup.profile.domain.model.ProfileRules;
import lombok.Getter;

import java.time.Instant;

@Getter
public class ProfileTopicAggregate {

    private final String topicId;

    private int experience;

    private int wins;
    private int losses;
    private int draws;

    private Instant createdAt;
    private Instant updatedAt;

    public ProfileTopicAggregate(String topicId) {
        this.topicId = topicId;
        this.experience = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.createdAt = Instant.now();
    }

    public void addGame(ProfileGame game) {
        experience += ProfileRules.computeXpEarned(game.playerScore(), game.result());
        switch (game.result()) {
            case WIN -> wins++;
            case LOSS -> losses++;
            case DRAW -> draws++;
        }
        updatedAt = Instant.now();
    }
}
