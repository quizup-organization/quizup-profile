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
    void exploringTopicsEarnsExplorerTiers() {
        // Sujets joués : Fer 1, bronze 5, argent 10, or 20, platine 40, émeraude 70, diamant 103.
        Set<Badge> first = BadgeRules.evaluate(
                facts(0, false, 0, 0, 0, 0, 0, 5), Set.of());
        assertEquals(Set.of(Badge.EXPLORER_FER, Badge.EXPLORER_BRONZE, Badge.COLLECTOR_FER), first);

        Set<Badge> half = BadgeRules.evaluate(facts(0, false, 0, 0, 0, 0, 0, 50), Set.of());
        assertEquals(Set.of(Badge.EXPLORER_FER, Badge.EXPLORER_BRONZE, Badge.EXPLORER_ARGENT,
                Badge.EXPLORER_OR, Badge.EXPLORER_PLATINE, Badge.COLLECTOR_FER), half);

        Set<Badge> all = BadgeRules.evaluate(facts(0, false, 0, 0, 0, 0, 0, 103), Set.of());
        assertTrue(all.containsAll(
                Badge.ofLadder(BadgeFamily.EXPLORATION, BadgeMetric.DISTINCT_TOPICS)));
    }

    @Test
    void aQuietDuelEarnsOnlyTheEntryBadges() {
        // Un premier duel humain gagne les deux paliers d'entrée (volume + maîtrise) et, en
        // cascade, la première pièce de collection. Rien d'autre.
        assertEquals(Set.of(Badge.DUELIST_FER, Badge.MASTER_FER, Badge.COLLECTOR_FER),
                BadgeRules.evaluate(quiet(), Set.of()));
    }

    @Test
    void firstWin_earnsFirstWinBadge() {
        Set<Badge> earned = BadgeRules.evaluate(facts(30, true, 0, 1, 1, 30, 1), Set.of());

        assertEquals(Set.of(Badge.FIRST_WIN, Badge.DUELIST_FER, Badge.WINNER_FER,
                Badge.MASTER_FER, Badge.COLLECTOR_FER), earned);
    }

    @Test
    void perfectScore_earnsPerfectBadge() {
        Set<Badge> earned = BadgeRules.evaluate(
                facts(ProgressionRules.PERFECT_SCORE, false, 0, 1, 0, 160, 0), Set.of());

        assertEquals(Set.of(Badge.PERFECT, Badge.DUELIST_FER, Badge.MASTER_FER,
                Badge.COLLECTOR_FER), earned);
    }

    @Test
    void lightningNeedsFiveFastAnswersInTheSameDuel() {
        assertEquals(Set.of(Badge.DUELIST_FER, Badge.MASTER_FER, Badge.COLLECTOR_FER),
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
        Set<Badge> first = BadgeRules.evaluate(facts(30, false, 0, 1, 0, 30, 0), Set.of());
        assertTrue(first.contains(Badge.DUELIST_FER));
        assertFalse(first.contains(Badge.DUELIST_BRONZE));

        Set<Badge> bronze = BadgeRules.evaluate(facts(30, false, 0, 3, 0, 30, 0), Set.of());
        assertTrue(bronze.contains(Badge.DUELIST_BRONZE));
        assertFalse(bronze.contains(Badge.DUELIST_ARGENT));

        Set<Badge> silver = BadgeRules.evaluate(facts(30, false, 0, 10, 0, 30, 0), Set.of());
        assertTrue(silver.contains(Badge.DUELIST_ARGENT));
        assertFalse(silver.contains(Badge.DUELIST_OR));

        Set<Badge> gold = BadgeRules.evaluate(facts(30, false, 0, 30, 0, 30, 0), Set.of());
        assertTrue(gold.contains(Badge.DUELIST_OR));
        assertFalse(gold.contains(Badge.DUELIST_PLATINE));

        Set<Badge> platinum = BadgeRules.evaluate(facts(30, false, 0, 100, 0, 30, 0), Set.of());
        assertTrue(platinum.contains(Badge.DUELIST_PLATINE));
        assertFalse(platinum.contains(Badge.DUELIST_EMERAUDE));

        Set<Badge> emerald = BadgeRules.evaluate(facts(30, false, 0, 300, 0, 30, 0), Set.of());
        assertTrue(emerald.contains(Badge.DUELIST_EMERAUDE));
        assertFalse(emerald.contains(Badge.DUELIST_DIAMANT));

        Set<Badge> diamond = BadgeRules.evaluate(facts(30, false, 0, 1_000, 0, 30, 0), Set.of());
        assertTrue(diamond.containsAll(Set.copyOf(
                Badge.ofLadder(BadgeFamily.VOLUME, BadgeMetric.HUMAN_DUELS))));
    }

    @Test
    void winnerThresholds_areCountedSeparatelyFromDuels() {
        Set<Badge> earned = BadgeRules.evaluate(facts(30, true, 0, 100, 10, 300, 1), Set.of());

        assertTrue(earned.contains(Badge.WINNER_FER));
        assertTrue(earned.contains(Badge.WINNER_BRONZE));
        assertTrue(earned.contains(Badge.WINNER_ARGENT));
        assertFalse(earned.contains(Badge.WINNER_OR));
        assertTrue(earned.contains(Badge.DUELIST_OR));
        assertTrue(earned.contains(Badge.DUELIST_PLATINE));
        assertFalse(earned.contains(Badge.DUELIST_EMERAUDE));
    }

    @Test
    void masteryThresholds_followTheTopicXp() {
        Set<Badge> apprentice = BadgeRules.evaluate(
                facts(30, false, 0, 20, 0, 2_499, 0), Set.of());
        assertTrue(apprentice.contains(Badge.MASTER_FER));
        assertTrue(apprentice.contains(Badge.MASTER_BRONZE));
        assertTrue(apprentice.contains(Badge.MASTER_ARGENT));
        assertFalse(apprentice.contains(Badge.MASTER_OR));

        assertTrue(BadgeRules.evaluate(facts(30, false, 0, 20, 0, 2_500, 0), Set.of())
                .contains(Badge.MASTER_OR));
        assertTrue(BadgeRules.evaluate(facts(30, false, 0, 50, 0, 6_000, 0), Set.of())
                .contains(Badge.MASTER_PLATINE));
        assertTrue(BadgeRules.evaluate(facts(30, false, 0, 250, 0, 30_000, 0), Set.of())
                .contains(Badge.MASTER_DIAMANT));
    }

    @Test
    void alreadyEarnedBadges_areNeverReAwarded() {
        Set<Badge> owned = EnumSet.of(Badge.FIRST_WIN, Badge.DUELIST_BRONZE);

        Set<Badge> earned = BadgeRules.evaluate(facts(30, true, 0, 10, 1, 30, 1), owned);

        assertFalse(earned.contains(Badge.FIRST_WIN));
        assertFalse(earned.contains(Badge.DUELIST_BRONZE));
        assertTrue(earned.contains(Badge.DUELIST_ARGENT));
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
        assertTrue(collectors.containsAll(Set.of(Badge.COLLECTOR_FER, Badge.COLLECTOR_BRONZE)),
                "au moins fer et bronze : " + collectors);
    }

    @Test
    void collectionCountsItsOwnTiersAndCascadesAtTheNextThreshold() {
        // Neuf badges possédés, dont le palier Fer de collection : le 30e duel humain ajoute
        // « Lame d'élite » et porte le total à 10, ce qui franchit « Collectionneur ».
        Set<Badge> owned = EnumSet.of(Badge.FIRST_WIN, Badge.PERFECT, Badge.LIGHTNING,
                Badge.STREAK_MASTER, Badge.DUELIST_FER, Badge.DUELIST_BRONZE,
                Badge.DUELIST_ARGENT, Badge.MASTER_FER, Badge.COLLECTOR_FER);

        Set<Badge> earned = BadgeRules.evaluate(facts(30, false, 0, 30, 0, 30, 0), owned);

        assertEquals(Set.of(Badge.DUELIST_OR, Badge.COLLECTOR_BRONZE), earned);
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
