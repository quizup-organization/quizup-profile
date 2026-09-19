package io.github.quizup.profile.domain.model;

import lombok.Builder;

import java.time.LocalDate;

/**
 * Activité journalière d'un joueur (read-model dérivé des fins de partie). Sert à calculer la
 * série de jours joués ({@code currentStreak} / {@code longestStreak}) exposée façon
 * « contributions ».
 */
@Builder(toBuilder = true)
public record PlayerActivity(
        String userId,
        int currentStreak,
        int longestStreak,
        LocalDate lastActiveDate
) {

    /** Activité neutre d'un joueur n'ayant encore joué aucune partie. */
    public static PlayerActivity empty(String userId) {
        return PlayerActivity.builder()
                .userId(userId)
                .currentStreak(0)
                .longestStreak(0)
                .lastActiveDate(null)
                .build();
    }
}
