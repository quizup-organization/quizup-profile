package io.github.quizup.profile.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgressionRulesTest {

    @Test
    void xpFor_addsWinBonusOnlyOnWin() {
        assertEquals(100, ProgressionRules.xpFor(100, false, false));
        assertEquals(150, ProgressionRules.xpFor(100, true, false));
    }

    @Test
    void xpFor_neverNegative() {
        assertEquals(0, ProgressionRules.xpFor(-10, false, false));
        assertEquals(50, ProgressionRules.xpFor(-10, true, false));
    }

    @Test
    void xpFor_botGameIsTrainingAndEarnsNoXp() {
        assertEquals(0, ProgressionRules.xpFor(160, true, true));
        assertEquals(0, ProgressionRules.xpFor(100, false, true));
    }
}
