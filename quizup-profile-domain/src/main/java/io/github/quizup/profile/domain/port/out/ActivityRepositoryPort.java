package io.github.quizup.profile.domain.port.out;

import io.github.quizup.profile.domain.model.ActivityDay;
import io.github.quizup.profile.domain.model.PlayerActivity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistance de l'activité journalière (read-model dérivé des fins de partie).
 */
public interface ActivityRepositoryPort {

    Optional<PlayerActivity> findActivity(String userId);

    void saveActivity(PlayerActivity activity);

    /**
     * Compte une partie pour un jour, de façon idempotente par {@code gameId}.
     *
     * @return {@code true} si la partie est nouvelle (compteur recalculé), {@code false} si déjà comptée.
     */
    boolean incrementDay(String userId, LocalDate date, String gameId);

    /** Points d'activité d'un joueur sur une fenêtre inclusive. */
    List<ActivityDay> findDays(String userId, LocalDate from, LocalDate to);
}
