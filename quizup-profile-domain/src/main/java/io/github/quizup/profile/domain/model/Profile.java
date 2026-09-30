package io.github.quizup.profile.domain.model;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
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
        String pseudonym,
        String bio,
        String country,
        String avatarOptions,
        Language language,
        Instant createdAt,
        Instant updatedAt
) {
}
