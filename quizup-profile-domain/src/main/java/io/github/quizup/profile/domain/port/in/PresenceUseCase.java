package io.github.quizup.profile.domain.port.in;

import io.github.quizup.profile.domain.model.PlayerPresence;

import java.util.List;

/**
 * Présence joueur : ouverture/fermeture de session temps réel, confirmation hors ligne à
 * l'échéance de grâce, lecture unitaire. La présence est un read-model éphémère (pas d'agrégat) :
 * le passage hors ligne est matérialisé par un TTL en base, confirmé par un balayeur périodique
 * (pas de deadline Axon, inadaptée à un simple délai de session).
 * La lecture groupée passe par {@link SearchPresenceUseCase} (recherche paginée standard).
 */
public interface PresenceUseCase {

    /**
     * Enregistre une session temps réel pour un joueur. La première session ouverte
     * bascule le joueur {@code ONLINE} et annule toute échéance hors ligne en cours.
     */
    PlayerPresence sessionConnected(String sessionId, String userId, String instanceId);

    /**
     * Ferme une session temps réel. La fermeture de la dernière session arme une échéance de
     * grâce ({@code offline_deadline_at}) ; le balayeur confirmera le passage {@code OFFLINE}
     * si aucune reconnexion n'intervient entre-temps.
     */
    PlayerPresence sessionDisconnected(String sessionId, String userId);

    /** Présence d'un joueur ; un joueur inconnu est retourné {@code OFFLINE}. */
    PlayerPresence get(String userId);

    /**
     * Purge les sessions d'une instance BFF ouvertes avant son démarrage ({@code startedAt}) et
     * arme l'échéance hors ligne des joueurs qui n'ont plus aucune session.
     */
    void resetInstanceSessions(String instanceId, java.time.Instant startedAt);

    /**
     * Confirme les passages hors ligne arrivés à échéance (aucune session restante) et publie
     * les événements {@code PlayerWentOfflineEvent} correspondants. Idempotent et sûr entre
     * instances (transition conditionnelle en base).
     */
    void expireOfflineDeadlines();

    /**
     * Renouvelle le bail ({@code last_seen_at}) des sessions fournies — heartbeat batch du BFF.
     */
    void renewSessions(List<String> sessionIds);

    /**
     * Supprime les sessions sans renouvellement depuis {@code PresenceRules.SESSION_LEASE_TTL}
     * (instance BFF disparue) et arme l'échéance hors ligne des joueurs qui n'ont plus de session.
     * Idempotent et sûr entre instances.
     */
    void expireStaleSessions();
}
