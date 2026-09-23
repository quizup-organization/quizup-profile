package io.github.quizup.profile.infrastructure.out.persistence.entity;

import io.github.quizup.microservice.core.domain.model.search.FieldType;
import io.github.quizup.microservice.core.domain.model.search.Searchable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * ProfileEntity - Entité JPA pour la projection du profil utilisateur
 * Cette table est une projection read-only mise à jour via les Event Handlers
 */
@Setter
@Getter
@Entity
@Table(name = "profile_entry", indexes = {
    @Index(name = "idx_profile_entry_email", columnList = "email"),
    @Index(name = "idx_profile_entry_display_name", columnList = "display_name")
})
public class ProfileEntity {

    @Id
    @Column(name = "user_id", length = 255, nullable = false)
    @Searchable(type = FieldType.STRING)
    private String userId;

    @Column(name = "email", length = 255, nullable = false, unique = true)
    @Searchable(type = FieldType.STRING)
    private String email;

    @Column(name = "display_name", length = 100, nullable = false)
    @Searchable(type = FieldType.STRING)
    private String displayName;

    @Column(name = "bio", length = 300)
    private String bio;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "avatar_options", length = 2000)
    private String avatarOptions;

    @Column(name = "created_at", nullable = false)
    @Searchable(type = FieldType.DATE)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @Searchable(type = FieldType.DATE)
    private Instant updatedAt;
}
