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
 */
public interface PresenceRules {

    Duration DISCONNECT_GRACE = Duration.ofSeconds(15);
}
