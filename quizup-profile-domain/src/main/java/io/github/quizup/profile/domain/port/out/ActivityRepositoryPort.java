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

    /** Incrémente le compteur de parties d'un jour (création à 1 s'il n'existe pas). */
    void incrementDay(String userId, LocalDate date);

    /** Points d'activité d'un joueur sur une fenêtre inclusive. */
    List<ActivityDay> findDays(String userId, LocalDate from, LocalDate to);
}
