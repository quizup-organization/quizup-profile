package io.github.quizup.profile.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EloRulesTest {

    @Test
    void initialScale_startsAtTwoHundred() {
        assertEquals(200, EloRules.INITIAL_RATING);
        assertTrue(EloRules.isProvisional(0));
        assertTrue(EloRules.isProvisional(9));
        assertFalse(EloRules.isProvisional(10));
    }

    @Test
    void kFactor_dropsAfterProvisionalGames() {
        assertEquals(40, EloRules.kFactor(0));
        assertEquals(40, EloRules.kFactor(9));
        assertEquals(20, EloRules.kFactor(10));
    }

    @Test
    void equalRatings_winGivesHalfTheK() {
        assertEquals(210, EloRules.newRating(200, 200, 1.0, 10));
        assertEquals(190, EloRules.newRating(200, 200, 0.0, 10));
        assertEquals(200, EloRules.newRating(200, 200, 0.5, 10));
    }

    @Test
    void expectedScore_favoursTheHigherRatedPlayer() {
        assertTrue(EloRules.expectedScore(1000, 800) > 0.7);
        assertTrue(EloRules.expectedScore(800, 1000) < 0.3);
    }

    @Test
    void upset_againstStrongerPlayerAwardsMore() {
        int upset = EloRules.newRating(200, 1000, 1.0, 10);
        int expectedWin = EloRules.newRating(1000, 200, 1.0, 10);

        assertTrue(upset - 200 > expectedWin - 1000);
        assertTrue(upset > 210);
    }

    @Test
    void ratingNeverGoesBelowZero() {
        assertEquals(0, EloRules.newRating(10, 0, 0.0, 0));
    }
}
