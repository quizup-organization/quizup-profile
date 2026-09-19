package io.github.quizup.profile.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionRulesTest {

    @Test
    void xpFor_addsWinBonusOnlyOnWin() {
        assertEquals(100, ProgressionRules.xpFor(100, false));
        assertEquals(150, ProgressionRules.xpFor(100, true));
    }

    @Test
    void xpFor_neverNegative() {
        assertEquals(0, ProgressionRules.xpFor(-10, false));
        assertEquals(50, ProgressionRules.xpFor(-10, true));
    }

    @Test
    void levelFor_startsAtOneAndGrowsByHundreds() {
        assertEquals(1, ProgressionRules.levelFor(0));
        assertEquals(1, ProgressionRules.levelFor(99));
        assertEquals(2, ProgressionRules.levelFor(100));
        assertEquals(2, ProgressionRules.levelFor(399));
        assertEquals(3, ProgressionRules.levelFor(400));
    }

    @Test
    void xpForNextLevel_matchesLevelInverse() {
        assertEquals(100, ProgressionRules.xpForNextLevel(1));
        assertEquals(400, ProgressionRules.xpForNextLevel(2));
        assertTrue(ProgressionRules.levelFor(ProgressionRules.xpForNextLevel(2)) == 3);
    }

    @Test
    void titleFor_mapsLevelBands() {
        assertEquals("Novice", ProgressionRules.titleFor(1));
        assertEquals("Apprenti", ProgressionRules.titleFor(2));
        assertEquals("Confirmé", ProgressionRules.titleFor(5));
        assertEquals("Expert", ProgressionRules.titleFor(10));
        assertEquals("Maître du savoir", ProgressionRules.titleFor(25));
    }
}
