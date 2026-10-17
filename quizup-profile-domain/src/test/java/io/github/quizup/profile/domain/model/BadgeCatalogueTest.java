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

    /** Chaque échelle (famille × métrique) porte ses **sept** paliers, du fer au diamant. */
    @Test
    void everyLadderHasTheSevenTiers() {
        Map<String, List<Badge>> ladders = java.util.Arrays.stream(Badge.values())
                .filter(badge -> !badge.isOneShot())
                .collect(Collectors.groupingBy(badge -> badge.family() + "/" + badge.metric()));

        assertEquals(12, ladders.size(), "12 échelles dans le catalogue");
        ladders.forEach((ladder, badges) -> {
            assertEquals(7, badges.size(), ladder + " doit porter sept paliers");
            List<Badge> sorted = badges.stream()
                    .sorted(Comparator.comparing(Badge::tier))
                    .toList();
            assertEquals(BadgeTier.FER, sorted.getFirst().tier(), ladder + " commence au fer");
            assertEquals(BadgeTier.DIAMANT, sorted.getLast().tier(), ladder + " finit au diamant");
        });
    }

    @Test
    void catalogueHasEightyEightBadges() {
        // 12 échelles × 7 paliers + 4 exploits ponctuels.
        assertEquals(88, Badge.values().length);
    }

    @Test
    void masteryBadgesFollowTheGradeLadder() {
        // Fer = première XP (Novice) ; les six paliers suivants reprennent les seuils des grades.
        assertEquals(1, Badge.MASTER_FER.target());
        assertEquals(MasteryGrade.APPRENTI.xpThreshold(), Badge.MASTER_BRONZE.target());
        assertEquals(MasteryGrade.PRATIQUANT.xpThreshold(), Badge.MASTER_ARGENT.target());
        assertEquals(MasteryGrade.CONFIRME.xpThreshold(), Badge.MASTER_OR.target());
        assertEquals(MasteryGrade.EXPERT.xpThreshold(), Badge.MASTER_PLATINE.target());
        assertEquals(MasteryGrade.MAITRE.xpThreshold(), Badge.MASTER_EMERAUDE.target());
        assertEquals(MasteryGrade.LEGENDE.xpThreshold(), Badge.MASTER_DIAMANT.target());
    }

    @Test
    void volumeBadgesMeasureHumanDuelsAndWins() {
        assertEquals(7, Badge.ofLadder(BadgeFamily.VOLUME, BadgeMetric.HUMAN_DUELS).size());
        // Les victoires ont leur **propre** famille : aucune famille ne mélange deux compteurs.
        assertEquals(7, Badge.ofLadder(BadgeFamily.VICTORY, BadgeMetric.HUMAN_WINS).size());
        assertTrue(Badge.ofFamily(BadgeFamily.VOLUME).stream()
                .allMatch(badge -> badge.metric() == BadgeMetric.HUMAN_DUELS));
        assertTrue(Badge.ofFamily(BadgeFamily.VICTORY).stream()
                .allMatch(badge -> badge.metric() == BadgeMetric.HUMAN_WINS));
    }

    @Test
    void everyFamilyCarriesExactlyOneLadder() {
        Map<BadgeFamily, List<Badge>> byFamily = java.util.Arrays.stream(Badge.values())
                .collect(Collectors.groupingBy(Badge::family));

        assertEquals(BadgeFamily.values().length, byFamily.size(), "chaque famille est peuplée");
        byFamily.forEach((family, badges) -> {
            long metrics = badges.stream().map(Badge::metric).distinct().count();
            assertEquals(1, metrics, family + " ne peut porter qu'une seule échelle");
        });
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
        assertEquals(0, BadgeTier.FER.ordinal());
        assertEquals(1, BadgeTier.BRONZE.ordinal());
        assertEquals(2, BadgeTier.ARGENT.ordinal());
        assertEquals(3, BadgeTier.OR.ordinal());
        assertEquals(4, BadgeTier.PLATINE.ordinal());
        assertEquals(5, BadgeTier.EMERAUDE.ordinal());
        assertEquals(6, BadgeTier.DIAMANT.ordinal());
        assertTrue(BadgeTier.DIAMANT.isHighest());
        assertFalse(BadgeTier.EMERAUDE.isHighest());
        assertFalse(BadgeTier.FER.isHighest());
    }
}
