package io.github.quizup.profile.infrastructure.in.api.response;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO de l'activité journalière d'un joueur : série courante/record, dernier jour actif et
 * détail des jours joués sur la fenêtre demandée (graphe de contribution).
 */
public record ActivityResponse(
        String userId,
        int currentStreak,
        int longestStreak,
        LocalDate lastActiveDate,
        int totalActiveDays,
        List<ActivityDayResponse> days
) implements Serializable {
}
