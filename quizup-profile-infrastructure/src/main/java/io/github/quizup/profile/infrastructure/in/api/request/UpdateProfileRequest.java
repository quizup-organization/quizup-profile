package io.github.quizup.profile.infrastructure.in.api.request;

/**
 * DTO de mise à jour d'un profil (displayName obligatoire, bio/country optionnels)
 */
public record UpdateProfileRequest(
        String displayName,
        String bio,
        String country
) {
}
