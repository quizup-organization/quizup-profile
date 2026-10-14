package io.github.quizup.profile.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void levelFor_followsTheAcceleratedCurve() {
        assertEquals(1, ProgressionRules.levelFor(0));
        assertEquals(1, ProgressionRules.levelFor(79));
        assertEquals(2, ProgressionRules.levelFor(80));
        assertEquals(2, ProgressionRules.levelFor(259));
        assertEquals(3, ProgressionRules.levelFor(260));
        assertEquals(5, ProgressionRules.levelFor(920));
        assertEquals(10, ProgressionRules.levelFor(4_320));
        assertEquals(20, ProgressionRules.levelFor(18_620));
    }

    @Test
    void xpForNextLevel_matchesLevelInverse() {
        // XP **totale** à atteindre pour le niveau suivant (contrat inchangé).
        assertEquals(80, ProgressionRules.xpForNextLevel(1));
        assertEquals(260, ProgressionRules.xpForNextLevel(2));
        assertEquals(920, ProgressionRules.xpForNextLevel(4));
        assertTrue(ProgressionRules.levelFor(ProgressionRules.xpForNextLevel(2)) == 3);
    }

    @Test
    void curve_isMonotonic_andThresholdsAreExact() {
        for (int level = 1; level < 40; level++) {
            int current = ProgressionRules.xpThreshold(level);
            int next = ProgressionRules.xpThreshold(level + 1);
            assertTrue(next > current, "seuil croissant au niveau " + level);
            assertEquals(level, ProgressionRules.levelFor(current), "seuil exact au niveau " + level);
            assertEquals(level, ProgressionRules.levelFor(next - 1), "dernier XP du niveau " + level);
        }
    }

    @Test
    void curve_staysAcceleratedOnTheFirstLevels() {
        // Début accéléré : ~1 duel pour N2, ~8 pour N5 (référence 120 XP par duel).
        assertTrue(ProgressionRules.xpThreshold(2) <= 120);
        assertTrue(ProgressionRules.xpThreshold(5) <= 8 * 120);
        assertTrue(ProgressionRules.xpThreshold(10) <= 36 * 120);
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
