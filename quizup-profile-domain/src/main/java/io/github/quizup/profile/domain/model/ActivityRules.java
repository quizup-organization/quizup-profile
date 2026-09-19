package io.github.quizup.profile.domain.model;

import java.time.LocalDate;

/**
 * Règles pures de la série de présence jouée (streak journalière).
 *
 * <p>Un jour actif (au moins une partie) prolonge la série si le dernier jour actif est la
 * veille, relance la série à 1 sinon, et ne change rien s'il a déjà été pris en compte. Le
 * calcul est idempotent : rejouer un événement du même jour n'incrémente pas la série.</p>
 */
public final class ActivityRules {

    private ActivityRules() {
    }

    public static PlayerActivity advance(PlayerActivity current, LocalDate activeDate) {
        if (activeDate == null || activeDate.equals(current.lastActiveDate())) {
            return current;
        }

        boolean consecutive = current.lastActiveDate() != null
                && current.lastActiveDate().plusDays(1).equals(activeDate);
        int nextStreak = consecutive ? current.currentStreak() + 1 : 1;

        return current.toBuilder()
                .currentStreak(nextStreak)
                .longestStreak(Math.max(current.longestStreak(), nextStreak))
                .lastActiveDate(activeDate)
                .build();
    }
}
