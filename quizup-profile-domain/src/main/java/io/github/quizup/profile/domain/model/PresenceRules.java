package io.github.quizup.profile.domain.model;

import java.time.Duration;

/**
 * Constantes de la présence joueur.
 *
 * <p>La présence est adossée à des <b>leases de session</b> dans le store chaud (Redis) : chaque
 * session STOMP est un lease à TTL, renouvelé par le BFF (heartbeat batch). L'absence de
 * renouvellement (crash d'instance, coupure réseau, half-open) expire le lease et déclenche le
 * passage {@code OFFLINE} après confirmation du balayeur — aucune session fantôme.</p>
 */
public interface PresenceRules {

    /** TTL d'un lease de session, renouvelé par le BFF. */
    Duration SESSION_LEASE_TTL = Duration.ofSeconds(30);

    /** Intervalle de renouvellement des leases côté BFF (1 commande batch par instance). */
    Duration SESSION_RENEW_INTERVAL = Duration.ofSeconds(10);

    /**
     * Grâce après la fermeture de la dernière session : absorbe les refresh et micro-coupures
     * sans provoquer de forfait injustifié.
     */
    Duration DISCONNECT_GRACE = Duration.ofSeconds(15);

    /**
     * Validité de l'état {@code ONLINE} sans renouvellement (lease + grâce) : au-delà, le
     * balayeur confirme le passage hors ligne (idempotent, multi-instances).
     */
    Duration ONLINE_LIVENESS = SESSION_LEASE_TTL.plus(DISCONNECT_GRACE);
}
