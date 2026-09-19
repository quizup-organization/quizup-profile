package io.github.quizup.profile.infrastructure.out.persistence.entity;

import io.github.quizup.microservice.core.domain.model.search.FieldType;
import io.github.quizup.microservice.core.domain.model.search.Searchable;
import io.github.quizup.profile.domain.model.PresenceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Entité JPA de la présence joueur (read-model éphémère, mis à jour par le cycle de vie des
 * sessions temps réel). Champs {@code @Searchable} : filtrage/tri standards via
 * {@code POST /api/presence/search}.
 */
@Getter
@Setter
@Entity
@Table(name = "presence_entry", indexes = {
        @Index(name = "idx_presence_entry_status", columnList = "status"),
        @Index(name = "idx_presence_entry_last_seen", columnList = "last_seen_at")
})
public class PresenceEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    @Searchable(type = FieldType.STRING)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private PresenceStatus status;

    @Column(name = "last_seen_at")
    @Searchable(type = FieldType.DATE)
    private Instant lastSeenAt;
}
