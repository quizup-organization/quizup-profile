package io.github.quizup.profile.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTitleTest {

    @Test
    void key_roundTripsThroughParse() {
        PlayerTitle title = new PlayerTitle("topic-1", MasteryGrade.CONFIRME);

        assertEquals("topic-1:CONFIRME", title.key());
        assertEquals(Optional.of(title), PlayerTitle.parse(title.key()));
    }

    @Test
    void parse_readsTheLastSeparator() {
        // Les identifiants de sujet n'ont pas de séparateur, mais on ne veut jamais couper trop tôt.
        assertEquals(
                Optional.of(new PlayerTitle("a:b", MasteryGrade.EXPERT)),
                PlayerTitle.parse("a:b:EXPERT"));
    }

    @Test
    void parse_rejectsMalformedOrUnknownKeys() {
        assertEquals(Optional.empty(), PlayerTitle.parse(null));
        assertEquals(Optional.empty(), PlayerTitle.parse("   "));
        assertEquals(Optional.empty(), PlayerTitle.parse("topic-1"));
        assertEquals(Optional.empty(), PlayerTitle.parse(":CONFIRME"));
        assertEquals(Optional.empty(), PlayerTitle.parse("topic-1:"));
        assertEquals(Optional.empty(), PlayerTitle.parse("topic-1:GRAND_MAITRE"));
    }

    @Test
    void rank_isDerivedFromTheGrade() {
        assertEquals(TitleRank.FER, new PlayerTitle("topic-1", MasteryGrade.NOVICE).rank());
        assertEquals(TitleRank.OR, new PlayerTitle("topic-1", MasteryGrade.CONFIRME).rank());
        assertEquals(TitleRank.DIAMANT, new PlayerTitle("topic-1", MasteryGrade.LEGENDE).rank());
    }

    @Test
    void label_appendsTheTopicNameAndFallsBackToTheGradeAlone() {
        PlayerTitle title = new PlayerTitle("topic-1", MasteryGrade.CONFIRME);

        assertEquals("Confirmé · Histoire", title.label("Histoire"));
        assertEquals("Confirmé", title.label(null));
        assertEquals("Confirmé", title.label("  "));
    }

    @Test
    void constructor_rejectsIncompleteTitles() {
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerTitle("  ", MasteryGrade.CONFIRME));
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerTitle("topic-1", null));
    }

    @Test
    void requireValid_failsOnMalformedKeys() {
        assertEquals("topic-1:LEGENDE", PlayerTitle.requireValid("topic-1:LEGENDE").key());
        assertThrows(IllegalArgumentException.class, () -> PlayerTitle.requireValid("topic-1"));
    }

    @Test
    void maxKeyLength_coversTheWidestTitle() {
        int longestGrade = java.util.Arrays.stream(MasteryGrade.values())
                .mapToInt(grade -> grade.name().length())
                .max()
                .orElse(0);

        assertTrue(PlayerTitle.maxKeyLength() >= 36 + 1 + longestGrade);
    }
}
