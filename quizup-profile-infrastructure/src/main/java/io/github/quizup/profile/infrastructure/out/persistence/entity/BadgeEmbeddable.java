package io.github.quizup.profile.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Embeddable
public class BadgeEmbeddable {

    @Column(name = "unlocked_at", nullable = false)
    private Instant unlockedAt;
}

