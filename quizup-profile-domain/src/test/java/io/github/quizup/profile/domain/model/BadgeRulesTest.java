package io.github.quizup.profile.domain.model;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
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
                                              int winStreakInTopic) {
        return facts(gameScore, won, fastAnswers, humanDuels, humanWins, topicXp, winStreakInTopic, 0);
    }

    private static BadgeRules.DuelFacts facts(int gameScore, boolean won, int fastAnswers,
                                              int humanDuels, int humanWins, int topicXp,
                                              int winStreakInTopic, int distinctTopics) {
        return facts(gameScore, won, fastAnswers, humanDuels, humanWins, topicXp, winStreakInTopic,
                distinctTopics, 0, 0);
    }

    private static BadgeRules.DuelFacts facts(int gameScore, boolean won, int fastAnswers,
                                              int humanDuels, int humanWins, int topicXp,
                                              int winStreakInTopic, int distinctTopics,
                                              int perfectDuels, int fastAnswersTotal) {
        return facts(gameScore, won, fastAnswers, humanDuels, humanWins, topicXp, winStreakInTopic,
                distinctTopics, perfectDuels, fastAnswersTotal, 0);
    }

    private static BadgeRules.DuelFacts facts(int gameScore, boolean won, int fastAnswers,
                                              int humanDuels, int humanWins, int topicXp,
                                              int winStreakInTopic, int distinctTopics,
                                              int perfectDuels, int fastAnswersTotal,
                                              int dayStreak) {
        return facts(gameScore, won, fastAnswers, humanDuels, humanWins, topicXp, winStreakInTopic,
                distinctTopics, perfectDuels, fastAnswersTotal, dayStreak, 0);
    }

    private static BadgeRules.DuelFacts facts(int gameScore, boolean won, int fastAnswers,
                                              int humanDuels, int humanWins, int topicXp,
                                              int winStreakInTopic, int distinctTopics,
                                              int perfectDuels, int fastAnswersTotal,
                                              int dayStreak, int imageCorrect) {
        return new BadgeRules.DuelFacts(
                gameScore, won, fastAnswers, humanDuels, humanWins, topicXp, winStreakInTopic,
                distinctTopics, perfectDuels, fastAnswersTotal, dayStreak, imageCorrect);
    }

    @Test
    void exploringTopicsEarnsExplorerTiers() {
        // Sujets joués : 5 bronze, 10 argent, 20 or, 40 platine, 70 émeraude, 103 diamant.
        Set<Badge> first = BadgeRules.evaluate(
                facts(0, false, 0, 0, 0, 0, 0, 5), Set.of());
        assertEquals(Set.of(Badge.EXPLORER_BRONZE), first);

        Set<Badge> half = BadgeRules.evaluate(facts(0, false, 0, 0, 0, 0, 0, 50), Set.of());
        assertEquals(Set.of(Badge.EXPLORER_BRONZE, Badge.EXPLORER_ARGENT, Badge.EXPLORER_OR,
                Badge.EXPLORER_PLATINE), half);

        Set<Badge> all = BadgeRules.evaluate(facts(0, false, 0, 0, 0, 0, 0, 103), Set.of());
        assertTrue(all.containsAll(
                Badge.ofLadder(BadgeFamily.EXPLORATION, BadgeMetric.DISTINCT_TOPICS)));
    }

    @Test
    void aQuietDuelEarnsNothing() {
        assertEquals(Set.of(), BadgeRules.evaluate(quiet(), Set.of()));
    }

    @Test
    void firstWin_earnsFirstWinBadge() {
        Set<Badge> earned = BadgeRules.evaluate(facts(30, true, 0, 1, 1, 30, 1), Set.of());

        assertEquals(Set.of(Badge.FIRST_WIN), earned);
    }

    @Test
    void perfectScore_earnsPerfectBadge() {
        Set<Badge> earned = BadgeRules.evaluate(
                facts(ProgressionRules.PERFECT_SCORE, false, 0, 1, 0, 160, 0), Set.of());

        assertEquals(Set.of(Badge.PERFECT), earned);
    }

    @Test
    void lightningNeedsFiveFastAnswersInTheSameDuel() {
        assertEquals(Set.of(),
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
    void volumeThresholds_unlockTierByTier() {
        assertFalse(BadgeRules.evaluate(facts(30, false, 0, 9, 0, 30, 0), Set.of())
                .contains(Badge.DUELIST_BRONZE));

        Set<Badge> bronze = BadgeRules.evaluate(facts(30, false, 0, 10, 0, 30, 0), Set.of());
        assertEquals(Set.of(Badge.DUELIST_BRONZE), bronze);

        Set<Badge> silver = BadgeRules.evaluate(facts(30, false, 0, 50, 0, 30, 0), Set.of());
        assertEquals(Set.of(Badge.DUELIST_BRONZE, Badge.DUELIST_ARGENT), silver);

        Set<Badge> gold = BadgeRules.evaluate(facts(30, false, 0, 100, 0, 30, 0), Set.of());
        assertEquals(
                Set.of(Badge.DUELIST_BRONZE, Badge.DUELIST_ARGENT, Badge.DUELIST_OR), gold);

        Set<Badge> emerald = BadgeRules.evaluate(facts(30, false, 0, 500, 0, 30, 0), Set.of());
        assertTrue(emerald.containsAll(Set.of(Badge.DUELIST_BRONZE, Badge.DUELIST_ARGENT,
                Badge.DUELIST_OR, Badge.DUELIST_PLATINE, Badge.DUELIST_EMERAUDE)));

        Set<Badge> diamond = BadgeRules.evaluate(facts(30, false, 0, 1_000, 0, 30, 0), Set.of());
        assertTrue(diamond.containsAll(Set.copyOf(
                Badge.ofLadder(BadgeFamily.VOLUME, BadgeMetric.HUMAN_DUELS))));
    }

    @Test
    void winnerThresholds_areCountedSeparatelyFromDuels() {
        Set<Badge> earned = BadgeRules.evaluate(facts(30, true, 0, 100, 10, 300, 1), Set.of());

        assertTrue(earned.contains(Badge.WINNER_BRONZE));
        assertFalse(earned.contains(Badge.WINNER_ARGENT));
        assertTrue(earned.contains(Badge.DUELIST_BRONZE));
        assertTrue(earned.contains(Badge.DUELIST_ARGENT));
    }

    @Test
    void masteryThresholds_followTheTopicXp() {
        assertFalse(BadgeRules.evaluate(facts(30, false, 0, 20, 0, 2_499, 0), Set.of())
                .contains(Badge.MASTER_BRONZE));

        assertTrue(BadgeRules.evaluate(facts(30, false, 0, 20, 0, 2_500, 0), Set.of())
                .contains(Badge.MASTER_BRONZE));
        assertTrue(BadgeRules.evaluate(facts(30, false, 0, 50, 0, 6_000, 0), Set.of())
                .contains(Badge.MASTER_ARGENT));
        assertTrue(BadgeRules.evaluate(facts(30, false, 0, 250, 0, 30_000, 0), Set.of())
                .contains(Badge.MASTER_OR));
    }

    @Test
    void alreadyEarnedBadges_areNeverReAwarded() {
        Set<Badge> owned = EnumSet.of(Badge.FIRST_WIN, Badge.DUELIST_BRONZE);

        Set<Badge> earned = BadgeRules.evaluate(facts(30, true, 0, 10, 1, 30, 1), owned);

        assertFalse(earned.contains(Badge.FIRST_WIN));
        assertFalse(earned.contains(Badge.DUELIST_BRONZE));
    }

    @Test
    void collectionCascade_unlocksEveryCrossedTierInOneAward() {
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
        assertTrue(collectors.containsAll(Set.of(Badge.COLLECTOR_BRONZE, Badge.COLLECTOR_ARGENT)),
                "au moins bronze et argent : " + collectors);
    }

    @Test
    void collectionCountsItsOwnTiers() {
        // 8 possédés + 1 nouveau fait = 9 (< 10) ; le palier bronze (5) est déjà là.
        Set<Badge> owned = EnumSet.of(Badge.FIRST_WIN, Badge.PERFECT, Badge.LIGHTNING,
                Badge.STREAK_MASTER, Badge.DUELIST_BRONZE, Badge.DUELIST_ARGENT, Badge.DUELIST_OR,
                Badge.COLLECTOR_BRONZE);

        Set<Badge> earned = BadgeRules.evaluate(facts(30, false, 0, 30, 0, 30, 0), owned);

        assertEquals(Set.of(), earned);
    }

    @Test
    void trainingStillEarnsDuelFacts() {
        // Un entraînement (compteurs entre joueurs inchangés) ne fait progresser ni le volume ni
        // la maîtrise, mais ses exploits restent des faits du duel.
        Set<Badge> earned = BadgeRules.evaluate(facts(160, true, 5, 0, 0, 0, 0), Set.of());

        assertEquals(Set.of(Badge.FIRST_WIN, Badge.PERFECT, Badge.LIGHTNING), earned);
    }
}
