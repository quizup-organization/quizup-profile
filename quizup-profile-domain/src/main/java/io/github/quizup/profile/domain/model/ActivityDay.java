package io.github.quizup.profile.domain.model;

import java.time.LocalDate;

/**
 * Nombre de parties jouées par un joueur sur une journée donnée (point du graphe d'activité).
 */
public record ActivityDay(
        LocalDate date,
        int games
) {
}
