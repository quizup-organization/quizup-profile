package io.github.quizup.profile.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Session temps réel STOMP d'un joueur (read-model éphémère), rattachée à l'instance BFF qui la
 * porte : la présence est dérivée du nombre de sessions ouvertes, et un redémarrage d'instance
 * purge uniquement les siennes.
 */
@Getter
@Setter
@Entity
@Table(name = "presence_session", indexes = {
        @Index(name = "idx_presence_session_user", columnList = "user_id"),
        @Index(name = "idx_presence_session_instance", columnList = "instance_id")
})
public class PresenceSessionEntity {

    @Id
    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "instance_id")
    private String instanceId;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt;
}
