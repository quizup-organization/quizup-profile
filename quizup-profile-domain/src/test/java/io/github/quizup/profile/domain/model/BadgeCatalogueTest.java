package io.github.quizup.profile.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Invariants du catalogue de badges : cible positive, paliers croissants dans une famille, et
 * **codes historiques préservés** (ils sont persistés dans les read models des joueurs).
 */
class BadgeCatalogueTest {

    @Test
    void historicalBadgeCodesArePreserved() {
        for (String code : List.of("FIRST_WIN", "PERFECT", "LIGHTNING", "STREAK_MASTER")) {
            assertDoesNotThrow(() -> Badge.valueOf(code),
                    "code persisté conservé : " + code);
        }
    }

    @Test
    void everyBadgeHasAPositiveTarget() {
        for (Badge badge : Badge.values()) {
            assertTrue(badge.target() > 0, "cible positive pour " + badge);
            assertFalse(badge.label().isBlank(), "libellé pour " + badge);
        }
    }

    /** Hors faits ponctuels, la cible doit croître strictement avec le palier. */
    @Test
    void targetsIncreaseWithTierWithinEachFamilyAndMetric() {
        Map<String, List<Badge>> groups = java.util.Arrays.stream(Badge.values())
                .filter(badge -> !badge.isOneShot())
                .collect(Collectors.groupingBy(badge -> badge.family() + "/" + badge.metric()));

        assertFalse(groups.isEmpty());
        groups.forEach((group, badges) -> {
            List<Badge> sorted = badges.stream()
                    .sorted(Comparator.comparing(Badge::tier))
                    .toList();
            int previousTarget = 0;
            for (Badge badge : sorted) {
                assertTrue(badge.target() > previousTarget,
                        "cible croissante dans " + group + " (" + badge + ")");
                previousTarget = badge.target();
            }
        });
    }

    @Test
    void masteryBadgesTargetTheGradeThresholds() {
        assertEquals(MasteryGrade.CONFIRME.xpThreshold(), Badge.MASTER_BRONZE.target());
        assertEquals(MasteryGrade.EXPERT.xpThreshold(), Badge.MASTER_ARGENT.target());
        assertEquals(MasteryGrade.LEGENDE.xpThreshold(), Badge.MASTER_OR.target());
    }

    @Test
    void volumeBadgesMeasureHumanDuelsAndWins() {
        assertEquals(3, Badge.ofFamily(BadgeFamily.VOLUME).stream()
                .filter(badge -> badge.metric() == BadgeMetric.HUMAN_DUELS)
                .count());
        assertEquals(3, Badge.ofFamily(BadgeFamily.VOLUME).stream()
                .filter(badge -> badge.metric() == BadgeMetric.HUMAN_WINS)
                .count());
    }

    @Test
    void oneShotBadgesAreExploits() {
        for (Badge badge : Badge.values()) {
            if (badge.isOneShot()) {
                assertEquals(BadgeFamily.EXPLOIT, badge.family(), badge + " est un exploit");
                assertEquals(1, badge.target(), badge + " n'a pas de progression");
            }
        }
    }

    @Test
    void ofFamily_isOrderedByTier() {
        for (BadgeFamily family : BadgeFamily.values()) {
            List<Badge> badges = Badge.ofFamily(family);
            assertFalse(badges.isEmpty(), "famille non vide : " + family);
            assertTrue(badges.stream().allMatch(badge -> badge.family() == family));
            for (int index = 1; index < badges.size(); index++) {
                assertTrue(badges.get(index - 1).tier().compareTo(badges.get(index).tier()) <= 0,
                        "paliers ordonnés dans " + family);
            }
        }
    }

    @Test
    void collectionBadgesAreReachableWithTheCurrentCatalogue() {
        long catalogueSize = Badge.values().length;
        for (Badge badge : Badge.ofFamily(BadgeFamily.COLLECTION)) {
            assertTrue(badge.target() <= catalogueSize,
                    badge + " doit être atteignable (catalogue de " + catalogueSize + " badges)");
        }
    }

    @Test
    void tiers_haveAStableOrder() {
        assertEquals(0, BadgeTier.BRONZE.ordinal());
        assertEquals(1, BadgeTier.ARGENT.ordinal());
        assertEquals(2, BadgeTier.OR.ordinal());
        assertTrue(BadgeTier.OR.isHighest());
        assertFalse(BadgeTier.BRONZE.isHighest());
    }
}
