package io.github.quizup.profile.domain.query;

import java.time.LocalDate;

/**
 * Queries Axon — activité journalière d'un joueur (streak + graphe de contribution).
 */
public interface ActivityQuery {

    /**
     * Activité sur la fenêtre inclusive {@code [from, to]}.
     */
    record GetActivityQuery(String userId, LocalDate from, LocalDate to) implements ActivityQuery {
    }
}
