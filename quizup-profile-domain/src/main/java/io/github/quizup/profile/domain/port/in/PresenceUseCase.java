package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.model.PlayerPresence;

import java.util.List;

/**
 * Présence joueur : leases de session temps réel (store chaud), lecture de la projection
 * durable et confirmation périodique des passages hors ligne.
 *
 * <p>La présence est un read-model éphémère (pas d'agrégat) : la vérité {@code ONLINE}/
 * {@code OFFLINE} vit dans le store chaud, la projection en base sert aux lectures produit.
 * Le passage hors ligne est confirmé par un balayeur périodique (pas de deadline Axon).</p>
 */
public interface PresenceUseCase {

    /**
     * Enregistre/renouvelle une session temps réel. La première session (ou la première après
     * passage hors ligne) bascule le joueur {@code ONLINE} et publie l'événement correspondant.
     */
    PlayerPresence sessionConnected(String sessionId, String userId, String instanceId);

    /**
     * Ferme une session temps réel. La fermeture de la dernière session vivante démarre la
     * grâce de déconnexion ; le balayeur confirmera le passage {@code OFFLINE} si aucune
     * reconnexion n'intervient.
     */
    PlayerPresence sessionDisconnected(String sessionId, String userId);

    /** Présence d'un joueur ; un joueur inconnu est retourné {@code OFFLINE}. */
    PlayerPresence get(String userId);

    /** Renouvelle en lot les leases des sessions encore vivantes (heartbeat batch du BFF). */
    void renewSessions(List<String> sessionIds);

    /**
     * Confirme les passages hors ligne arrivés à échéance (plus aucune session vivante) et
     * publie les événements {@code PlayerWentOfflineEvent} correspondants. Idempotent et sûr
     * entre instances (réclamation atomique dans le store chaud).
     */
    void expireOfflineDeadlines();
}
