package io.github.quizup.profile.domain.model;

/**
 * Échéances différées de la présence (une tâche ponctuelle par déconnexion, pas de boucle).
 */
public interface PresenceDeadline {

    /** Nom de l'échéance confirmant le passage hors ligne après le délai de grâce. */
    String OFFLINE = "presence-offline";

    /** Charge utile de l'échéance : le joueur dont la dernière session vient de se fermer. */
    record OfflineCheck(String userId) {
    }
}
