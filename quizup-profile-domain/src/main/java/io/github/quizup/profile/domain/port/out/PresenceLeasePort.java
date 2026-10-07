package io.github.quizup.profile.domain.port.out;

import io.github.quizup.profile.domain.model.PresenceRules;

import java.time.Instant;
import java.util.List;

/**
 * Leases de session temps réel dans le store chaud (Redis) : <b>source de vérité</b> de la
 * présence {@code ONLINE}/{@code OFFLINE}. Chaque session est un lease à TTL
 * ({@link PresenceRules#SESSION_LEASE_TTL}), renouvelé par le BFF tant que la connexion vit.
 * Un lease non renouvelé (crash d'instance, coupure) expire et le joueur est repris par
 * {@link #claimDueOffline(Instant)}.
 */
public interface PresenceLeasePort {

    /**
     * Ouvre (ou rouvre) la session : lease à TTL, joueur {@code ONLINE}, échéance repoussée.
     *
     * @return {@code true} si la session a fait basculer le joueur hors ligne → en ligne
     *         (transition à publier).
     */
    boolean openSession(String userId, String sessionId);

    /**
     * Ferme la session. Si une autre session du joueur est encore vivante, aucun délai n'est
     * armé ; sinon la grâce de déconnexion ({@link PresenceRules#DISCONNECT_GRACE}) démarre.
     *
     * @return {@code true} si une autre session du joueur est encore vivante.
     */
    boolean closeSession(String userId, String sessionId);

    /**
     * Renouvelle en lot les leases des sessions encore vivantes (heartbeat batch du BFF) :
     * seules les sessions connues du store sont prolongées (les mortes ne ressuscitent pas).
     */
    void renewSessions(List<String> sessionIds);

    /**
     * Réclame atomiquement les joueurs dont l'échéance est dépassée et qui n'ont plus aucune
     * session vivante ; l'état chaud est basculé {@code OFFLINE} dans la même opération. Un
     * joueur n'est réclamé que par une seule instance (idempotent, sûr entre instances).
     */
    List<String> claimDueOffline(Instant now);
}
