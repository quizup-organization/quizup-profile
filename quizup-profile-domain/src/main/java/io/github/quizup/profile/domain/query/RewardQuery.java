package io.github.quizup.profile.domain.query;

/** Requêtes de la projection des récompenses. */
public interface RewardQuery {

    record GetPlayerRewardsQuery(String userId) implements RewardQuery {
    }
}
