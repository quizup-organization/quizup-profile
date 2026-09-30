package io.github.quizup.profile.infrastructure.out.persistence.mapper;

import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.model.ProfileRules;
import io.github.quizup.profile.infrastructure.out.persistence.entity.ProfileEntity;

/**
 * Mapper infrastructure - Convertit entre ProfileEntity (JPA) et Profile (domaine).
 * Seule classe autorisée à connaitre les deux types simultanément.
 */
public final class ProfileEntityMapper {

    /**
     * Convertit une entité JPA en modèle domaine.
     */
    public static Profile toDomain(ProfileEntity entity) {
        return Profile.builder()
                .userId(entity.getUserId())
                .email(entity.getEmail())
                .pseudonym(entity.getPseudonym())
                .bio(entity.getBio())
                .country(entity.getCountry())
                .avatarOptions(entity.getAvatarOptions())
                .language(toLanguage(entity.getLanguage()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Convertit un modèle domaine en entité JPA.
     */
    public static ProfileEntity toEntity(Profile profile) {
        ProfileEntity entity = new ProfileEntity();
        entity.setUserId(profile.userId());
        entity.setEmail(profile.email());
        entity.setPseudonym(profile.pseudonym());
        entity.setBio(profile.bio());
        entity.setCountry(profile.country());
        entity.setAvatarOptions(profile.avatarOptions());
        entity.setLanguage(profile.language() == null
                ? ProfileRules.DEFAULT_LANGUAGE.code()
                : profile.language().code());
        entity.setCreatedAt(profile.createdAt());
        entity.setUpdatedAt(profile.updatedAt());
        return entity;
    }

    private static Language toLanguage(String code) {
        if (code == null) {
            return ProfileRules.DEFAULT_LANGUAGE;
        }
        return Language.fromCode(code);
    }
}
