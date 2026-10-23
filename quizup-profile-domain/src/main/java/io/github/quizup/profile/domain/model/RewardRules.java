package io.github.quizup.profile.domain.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Catalogue des récompenses et de leurs déclencheurs. Le badge reste une réussite autonome :
 * c'est ce catalogue qui décide, côté récompenses, qu'un badge débloque un droit.
 */
public final class RewardRules {

    private RewardRules() {
    }

    /** Identifiant namespacé de l'agrégat des récompenses d'un joueur. */
    public static String rewardIdFor(String userId) {
        return "rewards:" + userId;
    }

    /** Récompense débloquée par un badge, si elle existe. */
    public static Optional<RewardCode> forBadge(Badge badge) {
        if (badge == Badge.ASSIDUITY_BRONZE) {
            return Optional.of(RewardCode.MICAH_AVATAR);
        }
        return Optional.empty();
    }

    /** Catalogue complet, y compris les récompenses non encore débloquées. */
    public static List<RewardDefinition> catalog() {
        return Arrays.stream(Badge.values())
                .flatMap(badge -> forBadge(badge)
                        .map(code -> new RewardDefinition(code, badge))
                        .stream())
                .toList();
    }
}
