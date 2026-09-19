package io.github.quizup.profile.domain.model;

import lombok.Builder;

import java.time.Instant;

/**
 * Modèle domaine représentant le profil modifiable d'un utilisateur.
 * L'email est dénormalisé depuis identity pour permettre la recherche.
 * Immuable par convention (record).
 */
@Builder(toBuilder = true)
public record Profile(
        String userId,
        String email,
        String displayName,
        String bio,
        String country,
        Instant createdAt,
        Instant updatedAt
) {
}
