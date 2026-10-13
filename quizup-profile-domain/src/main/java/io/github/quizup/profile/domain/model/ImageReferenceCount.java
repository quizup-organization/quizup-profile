package io.github.quizup.profile.domain.model;

/**
 * Nombre d'utilisations d'une URL d'image dans le read model profile
 * (bannière de profil), pour la console d'administration des médias du BFF.
 */
public record ImageReferenceCount(String imageUrl, long count) {
}
