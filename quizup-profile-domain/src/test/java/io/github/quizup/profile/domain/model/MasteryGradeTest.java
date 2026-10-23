package io.github.quizup.profile.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MasteryGradeTest {

    @Test
    void gradeFor_followsTheExplicitThresholds() {
        assertEquals(MasteryGrade.NOVICE, MasteryGrade.gradeFor(0));
        assertEquals(MasteryGrade.NOVICE, MasteryGrade.gradeFor(499));
        assertEquals(MasteryGrade.APPRENTI, MasteryGrade.gradeFor(500));
        assertEquals(MasteryGrade.APPRENTI, MasteryGrade.gradeFor(1_499));
        assertEquals(MasteryGrade.PRATIQUANT, MasteryGrade.gradeFor(1_500));
        assertEquals(MasteryGrade.CONFIRME, MasteryGrade.gradeFor(4_000));
        assertEquals(MasteryGrade.EXPERT, MasteryGrade.gradeFor(9_000));
        assertEquals(MasteryGrade.MAITRE, MasteryGrade.gradeFor(21_000));
        assertEquals(MasteryGrade.LEGENDE, MasteryGrade.gradeFor(45_000));
        assertEquals(MasteryGrade.LEGENDE, MasteryGrade.gradeFor(1_000_000));
    }

    @Test
    void thresholds_areStrictlyIncreasing() {
        MasteryGrade previous = MasteryGrade.NOVICE;
        for (MasteryGrade grade : MasteryGrade.values()) {
            assertTrue(grade.xpThreshold() >= previous.xpThreshold(),
                    "seuil croissant pour " + grade);
            previous = grade;
        }
        assertEquals(0, MasteryGrade.NOVICE.xpThreshold());
    }

    @Test
    void everyGradeCarriesItsTitleRank() {
        assertEquals(TitleRank.FER, MasteryGrade.NOVICE.rank());
        assertEquals(TitleRank.BRONZE, MasteryGrade.APPRENTI.rank());
        assertEquals(TitleRank.ARGENT, MasteryGrade.PRATIQUANT.rank());
        assertEquals(TitleRank.OR, MasteryGrade.CONFIRME.rank());
        assertEquals(TitleRank.PLATINE, MasteryGrade.EXPERT.rank());
        assertEquals(TitleRank.EMERAUDE, MasteryGrade.MAITRE.rank());
        assertEquals(TitleRank.DIAMANT, MasteryGrade.LEGENDE.rank());
        assertTrue(TitleRank.DIAMANT.isHighest());
        assertFalse(TitleRank.EMERAUDE.isHighest());
    }

    @Test
    void next_isChainedAndEndsAtLegende() {
        assertEquals(MasteryGrade.APPRENTI, MasteryGrade.NOVICE.next());
        assertNull(MasteryGrade.LEGENDE.next());
        assertTrue(MasteryGrade.LEGENDE.isLast());
        assertFalse(MasteryGrade.MAITRE.isLast());
    }

    @Test
    void progressAndRemaining_bracketTheCurrentGrade() {
        // Entre Confirmé (4 000) et Expert (9 000).
        assertEquals(0, MasteryGrade.CONFIRME.progressPercent(4_000));
        assertEquals(50, MasteryGrade.CONFIRME.progressPercent(6_500));
        assertEquals(100, MasteryGrade.CONFIRME.progressPercent(9_000));
        assertEquals(5_000, MasteryGrade.CONFIRME.remainingXp(4_000));
        assertEquals(0, MasteryGrade.CONFIRME.remainingXp(9_000));
    }

    @Test
    void lastGrade_isAlwaysComplete() {
        assertEquals(100, MasteryGrade.LEGENDE.progressPercent(45_000));
        assertEquals(0, MasteryGrade.LEGENDE.remainingXp(45_000));
    }

    @Test
    void fromKey_readsThePersistedGradeIgnoringCase() {
        assertEquals(Optional.of(MasteryGrade.CONFIRME), MasteryGrade.fromKey("CONFIRME"));
        assertEquals(Optional.of(MasteryGrade.CONFIRME), MasteryGrade.fromKey("confirme"));
        assertEquals(Optional.empty(), MasteryGrade.fromKey("GRAND_MAITRE"));
        assertEquals(Optional.empty(), MasteryGrade.fromKey(null));
        assertEquals(Optional.empty(), MasteryGrade.fromKey("  "));
    }
}
