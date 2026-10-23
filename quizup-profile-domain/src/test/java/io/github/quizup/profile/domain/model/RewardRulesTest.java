package io.github.quizup.profile.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RewardRulesTest {

    @Test
    void assiduityBronzeUnlocksMicah() {
        assertEquals(RewardCode.MICAH_AVATAR, RewardRules.forBadge(Badge.ASSIDUITY_BRONZE).orElseThrow());
    }

    @Test
    void catalogContainsMicahAndItsBadgeCondition() {
        assertEquals(1, RewardRules.catalog().size());
        assertEquals(RewardCode.MICAH_AVATAR, RewardRules.catalog().get(0).code());
        assertEquals(Badge.ASSIDUITY_BRONZE, RewardRules.catalog().get(0).badge());
    }

    @Test
    void otherBadgesDoNotUnlockRewards() {
        assertTrue(RewardRules.forBadge(Badge.FIRST_WIN).isEmpty());
        assertTrue(RewardRules.forBadge(Badge.ASSIDUITY_ARGENT).isEmpty());
    }
}
