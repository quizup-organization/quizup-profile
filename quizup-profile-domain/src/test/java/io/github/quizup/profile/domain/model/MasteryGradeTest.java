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
        assertEquals(MasteryGrade.NOVICE, MasteryGrade.gradeFor(299));
        assertEquals(MasteryGrade.APPRENTI, MasteryGrade.gradeFor(300));
        assertEquals(MasteryGrade.PRATIQUANT, MasteryGrade.gradeFor(2_499));
        assertEquals(MasteryGrade.CONFIRME, MasteryGrade.gradeFor(2_500));
        assertEquals(MasteryGrade.EXPERT, MasteryGrade.gradeFor(6_000));
        assertEquals(MasteryGrade.MAITRE, MasteryGrade.gradeFor(14_000));
        assertEquals(MasteryGrade.LEGENDE, MasteryGrade.gradeFor(30_000));
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
    void next_isChainedAndEndsAtLegende() {
        assertEquals(MasteryGrade.APPRENTI, MasteryGrade.NOVICE.next());
        assertNull(MasteryGrade.LEGENDE.next());
        assertTrue(MasteryGrade.LEGENDE.isLast());
        assertFalse(MasteryGrade.MAITRE.isLast());
    }

    @Test
    void progressAndRemaining_bracketTheCurrentGrade() {
        // Entre Confirmé (2 500) et Expert (6 000).
        assertEquals(0, MasteryGrade.CONFIRME.progressPercent(2_500));
        assertEquals(50, MasteryGrade.CONFIRME.progressPercent(4_250));
        assertEquals(100, MasteryGrade.CONFIRME.progressPercent(6_000));
        assertEquals(3_500, MasteryGrade.CONFIRME.remainingXp(2_500));
        assertEquals(0, MasteryGrade.CONFIRME.remainingXp(6_000));
    }

    @Test
    void lastGrade_isAlwaysComplete() {
        assertEquals(100, MasteryGrade.LEGENDE.progressPercent(30_000));
        assertEquals(0, MasteryGrade.LEGENDE.remainingXp(30_000));
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
