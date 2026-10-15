package io.github.quizup.profile.domain.model;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

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
        return new BadgeRules.DuelFacts(
                gameScore, won, fastAnswers, humanDuels, humanWins, topicXp, winStreakInTopic,
                distinctTopics, perfectDuels, fastAnswersTotal);
    }

    @Test
    void exploringTopicsEarnsExplorerTiers() {
        // 5 sujets joués → bronze ; 25 → argent ; 50 → or (famille EXPLORATION).
        Set<Badge> first = BadgeRules.evaluate(
                facts(0, false, 0, 0, 0, 0, 0, 5), Set.of());
        assertEquals(Set.of(Badge.EXPLORER_BRONZE), first);

        Set<Badge> all = BadgeRules.evaluate(facts(0, false, 0, 0, 0, 0, 0, 50), Set.of());
        assertEquals(Set.of(Badge.EXPLORER_BRONZE, Badge.EXPLORER_ARGENT, Badge.EXPLORER_OR), all);
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

        Set<Badge> silver = BadgeRules.evaluate(facts(30, false, 0, 100, 0, 30, 0), Set.of());
        assertEquals(Set.of(Badge.DUELIST_BRONZE, Badge.DUELIST_ARGENT), silver);

        Set<Badge> gold = BadgeRules.evaluate(facts(30, false, 0, 500, 0, 30, 0), Set.of());
        assertEquals(
                Set.of(Badge.DUELIST_BRONZE, Badge.DUELIST_ARGENT, Badge.DUELIST_OR), gold);
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
        // 4 badges possédés + 9 faits franchis = 13 → collection bronze (5) et argent (10) ;
        // ces deux paliers portent le total à 15, ce qui débloque l'or (15) dans la foulée.
        Set<Badge> owned = EnumSet.of(Badge.FIRST_WIN, Badge.PERFECT, Badge.LIGHTNING,
                Badge.STREAK_MASTER);

        Set<Badge> earned = BadgeRules.evaluate(facts(30, false, 0, 500, 500, 30_000, 0), owned);

        assertEquals(12, earned.size());
        assertTrue(earned.containsAll(Set.of(Badge.COLLECTOR_BRONZE, Badge.COLLECTOR_ARGENT,
                Badge.COLLECTOR_OR)));
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
