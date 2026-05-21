package io.github.quizup.profile.domain.aggregate;

import io.github.quizup.profile.domain.model.ProfileMatchResult;
import lombok.Getter;

/**
 * Sous-agregat qui encapsule les stats d'un profil pour un topic donne.
 */
@Getter
public class ProfileTopicProgression {

    private final String topicId;
    private int gamesPlayed;
    private int wins;
    private int losses;
    private int draws;
    private double winLossRatio;

    public ProfileTopicProgression(String topicId) {
        this.topicId = topicId;
        this.gamesPlayed = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.winLossRatio = 0.0d;
    }

    public void record(ProfileMatchResult result) {
        gamesPlayed++;
        switch (result) {
            case WIN -> wins++;
            case LOSS -> losses++;
            case DRAW -> draws++;
        }
        this.winLossRatio = calculateWinLossRatio(wins, losses);
    }

    private static double calculateWinLossRatio(int wins, int losses) {
        if (losses == 0) {
            return wins;
        }
        return (double) wins / losses;
    }
}

