package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.model.ActivityView;

import java.time.LocalDate;

/**
 * Cas d'usage — lecture de l'activité journalière d'un joueur.
 */
public interface GetActivityUseCase {

    /**
     * Activité d'un joueur sur la fenêtre inclusive {@code [from, to]}. Un joueur sans partie
     * retourne une activité neutre (série 0, aucun jour).
     */
    ActivityView get(String userId, LocalDate from, LocalDate to);
}
