package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.model.PlayerPresence;

/**
 * Présence joueur : ouverture/fermeture de session temps réel, lecture unitaire.
 * La présence est un read-model éphémère (pas d'agrégat ni d'événement de domaine).
 * La lecture groupée passe par {@link SearchPresenceUseCase} (recherche paginée standard).
 */
public interface PresenceUseCase {

    /**
     * Enregistre une session temps réel pour un joueur. La première session ouverte
     * bascule le joueur {@code ONLINE}.
     */
    PlayerPresence sessionConnected(String sessionId, String userId);

    /**
     * Ferme une session temps réel. La fermeture de la dernière session déclenche,
     * après un délai de grâce, le passage {@code OFFLINE} et l'événement de déconnexion.
     */
    PlayerPresence sessionDisconnected(String sessionId, String userId);

    /** Présence d'un joueur ; un joueur inconnu est retourné {@code OFFLINE}. */
    PlayerPresence get(String userId);

    /**
     * Confirme le passage hors ligne d'un joueur si plus aucune session n'est ouverte
     * (échéance de grâce après fermeture de la dernière session).
     */
    void confirmOffline(String userId);
}
