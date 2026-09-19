package io.github.quizup.profile.domain.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Vue complète de l'activité d'un joueur : série courante/record, dernier jour actif et
 * détail journalier sur la fenêtre demandée (pour le graphe de contribution).
 */
public record ActivityView(
        String userId,
        int currentStreak,
        int longestStreak,
        LocalDate lastActiveDate,
        int totalActiveDays,
        List<ActivityDay> days
) {
}
