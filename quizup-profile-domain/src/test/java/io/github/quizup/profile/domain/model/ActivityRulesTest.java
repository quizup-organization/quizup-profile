package io.github.quizup.profile.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActivityRulesTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 18);

    @Test
    void firstActiveDayStartsStreakAtOne() {
        PlayerActivity updated = ActivityRules.advance(PlayerActivity.empty("u1"), DAY);

        assertEquals(1, updated.currentStreak());
        assertEquals(1, updated.longestStreak());
        assertEquals(DAY, updated.lastActiveDate());
    }

    @Test
    void sameDayIsIdempotent() {
        PlayerActivity once = ActivityRules.advance(PlayerActivity.empty("u1"), DAY);
        PlayerActivity twice = ActivityRules.advance(once, DAY);

        assertEquals(1, twice.currentStreak());
        assertEquals(DAY, twice.lastActiveDate());
    }

    @Test
    void consecutiveDayExtendsStreak() {
        PlayerActivity day1 = ActivityRules.advance(PlayerActivity.empty("u1"), DAY);
        PlayerActivity day2 = ActivityRules.advance(day1, DAY.plusDays(1));

        assertEquals(2, day2.currentStreak());
        assertEquals(2, day2.longestStreak());
        assertEquals(DAY.plusDays(1), day2.lastActiveDate());
    }

    @Test
    void gapResetsCurrentStreakButKeepsLongest() {
        PlayerActivity day1 = ActivityRules.advance(PlayerActivity.empty("u1"), DAY);
        PlayerActivity day2 = ActivityRules.advance(day1, DAY.plusDays(1));
        PlayerActivity afterGap = ActivityRules.advance(day2, DAY.plusDays(4));

        assertEquals(1, afterGap.currentStreak());
        assertEquals(2, afterGap.longestStreak());
        assertEquals(DAY.plusDays(4), afterGap.lastActiveDate());
    }
}
