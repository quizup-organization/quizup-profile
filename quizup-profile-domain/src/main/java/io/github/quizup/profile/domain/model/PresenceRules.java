package io.github.quizup.profile.domain.model;

import java.time.Duration;

/**
 * Constantes de la présence joueur.
 *
 * <p>La présence est pilotée par le cycle de vie de la session temps réel STOMP : une
 * session ouverte bascule le joueur {@code ONLINE}, la fermeture de la dernière session
 * programme un passage {@code OFFLINE} après {@link #DISCONNECT_GRACE} (annulé par toute
 * reconnexion avant échéance). Ce délai absorbe les refresh et micro-coupures sans
 * provoquer de forfait injustifié.</p>
 *
 * <p><b>Filet de sécurité</b> : chaque session porte un bail ({@code last_seen_at}) renouvelé
 * par le BFF toutes les {@link #SESSION_RENEW_INTERVAL}. Une session sans renouvellement depuis
 * {@link #SESSION_LEASE_TTL} (crash d'instance, partition réseau, client mort) est considérée
 * comme fermée par le balayeur — aucune session fantôme ne maintient un joueur {@code ONLINE}.</p>
 */
public interface PresenceRules {

    Duration DISCONNECT_GRACE = Duration.ofSeconds(15);

    /** Validité d'un bail de session sans renouvellement avant purge par le balayeur. */
    Duration SESSION_LEASE_TTL = Duration.ofSeconds(30);

    /** Intervalle de renouvellement des bails côté BFF (une commande batch par instance). */
    Duration SESSION_RENEW_INTERVAL = Duration.ofSeconds(10);
}
