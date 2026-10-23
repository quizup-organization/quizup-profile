package io.github.quizup.profile.domain.model;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Règles pures d'attribution : seuils par métrique, exploits, cascade de collection et
 * non-réattribution.
 */
class BadgeRulesTest {

    /** Duel neutre : score modeste, perdu, sans réponse rapide, premier duel du joueur. */
    private static BadgeRules.DuelFacts quiet() {
        return facts(30, false, 0, 1, 0, 30, 0);
    }

    private static BadgeRules.DuelFacts facts(int gameScore, boolean won, int fastAnswers,
                                              int humanDuels, int humanWins, int topicXp,
                                              int winStreak) {
        return facts(gameScore, won, fastAnswers, humanDuels, humanWins, topicXp, winStreak, 0);
    }

    private static BadgeRules.DuelFacts facts(int gameScore, boolean won, int fastAnswers,
                                              int humanDuels, int humanWins, int topicXp,
                                              int winStreak, int distinctTopics) {
        return facts(gameScore, won, fastAnswers, humanDuels, humanWins, topicXp, winStreak,
                distinctTopics, 0, 0);
    }

    private static BadgeRules.DuelFacts facts(int gameScore, boolean won, int fastAnswers,
                                              int humanDuels, int humanWins, int topicXp,
                                              int winStreak, int distinctTopics,
                                              int perfectDuels, int fastAnswersTotal) {
        return facts(gameScore, won, fastAnswers, humanDuels, humanWins, topicXp, winStreak,
                distinctTopics, perfectDuels, fastAnswersTotal, 0);
    }

    private static BadgeRules.DuelFacts facts(int gameScore, boolean won, int fastAnswers,
                                              int humanDuels, int humanWins, int topicXp,
                                              int winStreak, int distinctTopics,
                                              int perfectDuels, int fastAnswersTotal,
                                              int dayStreak) {
        return facts(gameScore, won, fastAnswers, humanDuels, humanWins, topicXp, winStreak,
                distinctTopics, perfectDuels, fastAnswersTotal, dayStreak, 0);
    }

    private static BadgeRules.DuelFacts facts(int gameScore, boolean won, int fastAnswers,
                                              int humanDuels, int humanWins, int topicXp,
                                              int winStreak, int distinctTopics,
                                              int perfectDuels, int fastAnswersTotal,
                                              int dayStreak, int imageCorrect) {
        return facts(gameScore, won, fastAnswers, humanDuels, humanWins, topicXp, winStreak,
                distinctTopics, perfectDuels, fastAnswersTotal, dayStreak, imageCorrect, false);
    }

    private static BadgeRules.DuelFacts facts(int gameScore, boolean won, int fastAnswers,
                                              int humanDuels, int humanWins, int topicXp,
                                              int winStreak, int distinctTopics,
                                              int perfectDuels, int fastAnswersTotal,
                                              int dayStreak, int imageCorrect, boolean botGame) {
        return new BadgeRules.DuelFacts(
                gameScore, won, fastAnswers, humanDuels, humanWins, topicXp, winStreak,
                distinctTopics, perfectDuels, fastAnswersTotal, dayStreak, imageCorrect, botGame);
    }

    @Test
    void aFirstHumanDuelEarnsOnlyTheVolumeAndMasteryEntryBadges() {
        // Un premier duel humain (même perdu) débloque le volume Fer et la maîtrise Fer.
        // Exploration, précision, vision, série et collection restent hors d'atteinte.
        Set<Badge> expected = Set.of(Badge.DUELIST_FER, Badge.MASTER_FER);
        assertEquals(expected, BadgeRules.evaluate(quiet(), Set.of()));
        assertEquals(expected, BadgeRules.evaluate(
                facts(30, false, 0, 1, 0, 30, 0, 1), Set.of()));
    }

    @Test
    void aFirstWinEarnsExactlyThreeEntryBadges() {
        // Première victoire « normale » : volume + victoire + maîtrise. Pas de collection,
        // pas d'exploration (3 sujets), pas de précision (3 duels parfaits), pas de vision.
        assertEquals(Set.of(Badge.FIRST_WIN, Badge.DUELIST_FER, Badge.MASTER_FER),
                BadgeRules.evaluate(facts(30, true, 0, 1, 1, 30, 1, 1), Set.of()));
    }

    @Test
    void aPerfectAndLightningFirstWinEarnsAtMostFiveBadges() {
        Set<Badge> earned = BadgeRules.evaluate(
                facts(ProgressionRules.PERFECT_SCORE, true, 5, 1, 1, 210, 1, 1, 1, 5),
                Set.of());

        assertEquals(Set.of(Badge.FIRST_WIN, Badge.DUELIST_FER, Badge.MASTER_FER,
                Badge.PERFECT, Badge.LIGHTNING), earned);
        assertFalse(earned.contains(Badge.WINNER_FER));
        assertFalse(earned.contains(Badge.EXPLORER_FER));
        assertFalse(earned.contains(Badge.PRECISION_FER));
        assertFalse(earned.contains(Badge.VISION_FER));
        assertFalse(earned.contains(Badge.COLLECTOR_FER));
    }

    @Test
    void exploringTopicsEarnsExplorerTiers() {
        List<Badge> ladder = Badge.ofLadder(BadgeFamily.EXPLORATION, BadgeMetric.DISTINCT_TOPICS);
        int fer = Badge.EXPLORER_FER.target();
        int bronze = Badge.EXPLORER_BRONZE.target();

        assertFalse(BadgeRules.evaluate(
                facts(0, false, 0, 0, 0, 0, 0, fer - 1), Set.of())
                .contains(Badge.EXPLORER_FER));
        assertTrue(BadgeRules.evaluate(
                facts(0, false, 0, 0, 0, 0, 0, fer), Set.of())
                .contains(Badge.EXPLORER_FER));

        Set<Badge> atBronze = BadgeRules.evaluate(
                facts(0, false, 0, 0, 0, 0, 0, bronze), Set.of());
        assertTrue(atBronze.containsAll(ladder.subList(0, 2)));
        assertFalse(atBronze.contains(Badge.EXPLORER_ARGENT));
    }

    @Test
    void perfectScoreEarnsPerfectBadge() {
        assertEquals(Set.of(Badge.PERFECT, Badge.DUELIST_FER, Badge.MASTER_FER),
                BadgeRules.evaluate(
                        facts(ProgressionRules.PERFECT_SCORE, false, 0, 1, 0, 160, 0),
                        Set.of()));
    }

    @Test
    void lightningNeedsFiveFastAnswersInTheSameDuel() {
        assertEquals(Set.of(Badge.DUELIST_FER, Badge.MASTER_FER),
                BadgeRules.evaluate(facts(30, false, 4, 1, 0, 30, 0), Set.of()));
        assertTrue(BadgeRules.evaluate(facts(30, false, 5, 1, 0, 30, 0), Set.of())
                .contains(Badge.LIGHTNING));
    }

    @Test
    void streakMasterNeedsTenConsecutiveWinsInTheTopic() {
        assertFalse(BadgeRules.evaluate(facts(30, true, 0, 9, 9, 270, 9), Set.of())
                .contains(Badge.STREAK_MASTER));
        assertTrue(BadgeRules.evaluate(facts(30, true, 0, 10, 10, 300, 10), Set.of())
                .contains(Badge.STREAK_MASTER));
    }

    @Test
    void volumeThresholdsUnlockTierByTier() {
        for (Badge badge : Badge.ofLadder(BadgeFamily.VOLUME, BadgeMetric.HUMAN_DUELS)) {
            int target = badge.target();
            assertFalse(BadgeRules.evaluate(
                    facts(30, false, 0, target - 1, 0, 30, 0), Set.of())
                    .contains(badge), "avant " + badge);
            assertTrue(BadgeRules.evaluate(
                    facts(30, false, 0, target, 0, 30, 0), Set.of())
                    .contains(badge), "à " + badge);
        }
    }

    @Test
    void winnerThresholdsAreCountedSeparatelyFromDuels() {
        for (Badge badge : Badge.ofLadder(BadgeFamily.VICTORY, BadgeMetric.HUMAN_WINS)) {
            int target = badge.target();
            assertFalse(BadgeRules.evaluate(
                    facts(30, true, 0, 1_000, target - 1, 30, 1), Set.of())
                    .contains(badge), "avant " + badge);
            assertTrue(BadgeRules.evaluate(
                    facts(30, true, 0, 1_000, target, 30, 1), Set.of())
                    .contains(badge), "à " + badge);
        }
    }

    @Test
    void masteryThresholdsFollowTheTopicXp() {
        List<Badge> ladder = Badge.ofLadder(BadgeFamily.MASTERY, BadgeMetric.TOPIC_MASTERY);
        assertEquals(1, Badge.MASTER_FER.target());

        for (int index = 1; index < MasteryGrade.values().length; index++) {
            MasteryGrade grade = MasteryGrade.values()[index];
            Badge badge = ladder.get(index);
            assertEquals(grade.xpThreshold(), badge.target(), "alignement " + grade);

            assertFalse(BadgeRules.evaluate(
                    facts(30, false, 0, 0, 0, grade.xpThreshold() - 1, 0), Set.of())
                    .contains(badge), "avant " + badge);
            assertTrue(BadgeRules.evaluate(
                    facts(30, false, 0, 0, 0, grade.xpThreshold(), 0), Set.of())
                    .contains(badge), "à " + badge);
        }
    }

    @Test
    void alreadyEarnedBadgesAreNeverReAwarded() {
        Set<Badge> owned = EnumSet.of(Badge.FIRST_WIN, Badge.DUELIST_BRONZE);

        Set<Badge> earned = BadgeRules.evaluate(facts(
                30, true, 0, Badge.DUELIST_ARGENT.target(),
                Badge.WINNER_FER.target(), 30, 1), owned);

        assertFalse(earned.contains(Badge.FIRST_WIN));
        assertFalse(earned.contains(Badge.DUELIST_BRONZE));
        assertTrue(earned.contains(Badge.DUELIST_ARGENT));
        assertTrue(earned.contains(Badge.WINNER_FER));
    }

    @Test
    void collectionCascadeUnlocksEveryCrossedTierInOneAward() {
        Set<Badge> owned = EnumSet.of(Badge.FIRST_WIN, Badge.PERFECT, Badge.LIGHTNING,
                Badge.STREAK_MASTER);

        Set<Badge> earned = BadgeRules.evaluate(facts(30, false, 0, 500, 500, 30_000, 0), owned);

        // La collection se débloque en cascade : chaque palier franchi porte le total, qui peut
        // franchir le suivant. On vérifie contre le point fixe attendu (sans compte magique).
        Set<Badge> collectors = earned.stream()
                .filter(badge -> badge.family() == BadgeFamily.COLLECTION)
                .collect(Collectors.toSet());
        long others = earned.stream().filter(badge -> badge.family() != BadgeFamily.COLLECTION).count();

        Set<Badge> expected = EnumSet.noneOf(Badge.class);
        boolean changed = true;
        while (changed) {
            changed = false;
            int total = owned.size() + (int) others + expected.size();
            for (Badge collector : Badge.ofLadder(BadgeFamily.COLLECTION, BadgeMetric.EARNED_BADGES)) {
                if (collector.target() <= total && expected.add(collector)) {
                    changed = true;
                }
            }
        }

        assertEquals(expected, collectors);
        assertTrue(collectors.containsAll(Set.of(Badge.COLLECTOR_FER, Badge.COLLECTOR_BRONZE)),
                "au moins fer et bronze : " + collectors);
    }

    @Test
    void collectionCountsItsOwnTiersAndCascadesAtTheNextThreshold() {
        // Dix-neuf badges déjà acquis (dont aucun de collection) : le 3e sujet joué ajoute
        // « Explorateur » et porte le total à 20, ce qui franchit « Première pièce » (8) puis
        // « Collectionneur » (20) dans la même évaluation.
        Set<Badge> owned = EnumSet.of(
                Badge.FIRST_WIN, Badge.PERFECT, Badge.LIGHTNING, Badge.STREAK_MASTER,
                Badge.DUELIST_FER, Badge.DUELIST_BRONZE, Badge.DUELIST_ARGENT, Badge.DUELIST_OR,
                Badge.DUELIST_PLATINE, Badge.DUELIST_EMERAUDE,
                Badge.WINNER_FER, Badge.WINNER_BRONZE, Badge.WINNER_ARGENT,
                Badge.MASTER_FER, Badge.MASTER_BRONZE, Badge.MASTER_ARGENT,
                Badge.SERIES_FER, Badge.ASSIDUITY_FER, Badge.PRECISION_FER);

        Set<Badge> earned = BadgeRules.evaluate(
                facts(30, false, 0, 0, 0, 0, 0, Badge.EXPLORER_FER.target()), owned);

        assertEquals(Set.of(Badge.EXPLORER_FER, Badge.COLLECTOR_FER, Badge.COLLECTOR_BRONZE),
                earned);
    }

    @Test
    void trainingEarnsNoBadgeAtAll() {
        // Un entraînement contre bot n'acquiert **rien** : ni exploit (victoire, score maximal,
        // réponses rapides), ni compteur de duel — les badges et les titres ne s'obtiennent que
        // sur des parties **contre des humains**.
        assertEquals(Set.of(), BadgeRules.evaluate(
                facts(160, true, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, true), Set.of()));

        // …et ne retire jamais un badge déjà possédé.
        assertEquals(Set.of(), BadgeRules.evaluate(
                facts(160, true, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, true),
                Set.of(Badge.FIRST_WIN, Badge.PERFECT)));
    }
}
