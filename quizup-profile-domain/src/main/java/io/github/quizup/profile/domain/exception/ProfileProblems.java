package io.github.quizup.profile.domain.exception;

import io.github.quizup.microservice.core.domain.exception.ProblemCategory;
import io.github.quizup.profile.domain.model.ProfileRules;

import java.util.Map;


public interface ProfileProblems {

    class ProfileNotFoundProblem extends ProfileProblem {
        public ProfileNotFoundProblem(String userId) {
            super(userId, "urn:quizup:profile:notFound",
                    ProblemCategory.BUSINESS_RESOURCE_MISSING,
                    "Profile not found",
                    "The profile " + userId + " was not found", null);
        }
    }

    class ProfileNotOwnerProblem extends ProfileProblem {
        public ProfileNotOwnerProblem(String userId, String requestedBy) {
            super(userId, "urn:quizup:profile:notOwner",
                    ProblemCategory.PERMISSION,
                    "Profile update not allowed",
                    "Only the owner of the profile can update it",
                    Map.of("requestedBy", requestedBy));
        }
    }

    class PseudonymBlankProblem extends ProfileProblem {
        public PseudonymBlankProblem(String userId, String pseudonym) {
            super(userId, "urn:quizup:profile:blankPseudonym",
                    ProblemCategory.VALIDATION,
                    "Pseudonym is required",
                    "The pseudonym must not be blank",
                    Map.of("pseudonym", pseudonym));
        }
    }

    class PseudonymTooLongProblem extends ProfileProblem {
        public PseudonymTooLongProblem(String userId, String pseudonym) {
            super(userId, "urn:quizup:profile:pseudonymTooLong",
                    ProblemCategory.VALIDATION,
                    "Pseudonym too long",
                    "The pseudonym must not exceed " + ProfileRules.MAX_PSEUDONYM_LENGTH + " characters",
                    Map.of("pseudonym", pseudonym));
        }
    }

    class BioTooLongProblem extends ProfileProblem {
        public BioTooLongProblem(String userId, String bio) {
            super(userId, "urn:quizup:profile:bioTooLong",
                    ProblemCategory.VALIDATION,
                    "Bio too long",
                    "The bio must not exceed " + ProfileRules.MAX_BIO_LENGTH + " characters",
                    Map.of("bio", bio));
        }
    }

    class CountryTooLongProblem extends ProfileProblem {
        public CountryTooLongProblem(String userId, String country) {
            super(userId, "urn:quizup:profile:countryTooLong",
                    ProblemCategory.VALIDATION,
                    "Country too long",
                    "The country must not exceed " + ProfileRules.MAX_COUNTRY_LENGTH + " characters",
                    Map.of("country", country));
        }
    }

    class AvatarOptionsTooLongProblem extends ProfileProblem {
        public AvatarOptionsTooLongProblem(String userId) {
            super(userId, "urn:quizup:profile:avatarOptionsTooLong",
                    ProblemCategory.VALIDATION,
                    "Avatar options too long",
                    "The avatar options must not exceed " + ProfileRules.MAX_AVATAR_OPTIONS_LENGTH + " characters",
                    null);
        }
    }

    class BannerUrlTooLongProblem extends ProfileProblem {
        public BannerUrlTooLongProblem(String userId) {
            super(userId, "urn:quizup:profile:bannerUrlTooLong",
                    ProblemCategory.VALIDATION,
                    "Banner URL too long",
                    "The banner URL must not exceed " + ProfileRules.MAX_BANNER_URL_LENGTH + " characters",
                    null);
        }
    }

    class LanguageMissingProblem extends ProfileProblem {
        public LanguageMissingProblem(String userId) {
            super(userId, "urn:quizup:profile:languageMissing",
                    ProblemCategory.VALIDATION,
                    "Language is required",
                    "The language must be one of the supported languages",
                    null);
        }
    }

    /**
     * Clé de titre porté invalide (format attendu {@code topicId:GRADE}, cf. {@code PlayerTitle}).
     */
    class EquippedTitleInvalidProblem extends ProfileProblem {
        public EquippedTitleInvalidProblem(String userId, String titleKey) {
            super(userId, "urn:quizup:profile:invalidEquippedTitle",
                    ProblemCategory.VALIDATION,
                    "Invalid title",
                    "The equipped title must be a valid title key (topicId:GRADE)",
                    Map.of("titleKey", String.valueOf(titleKey)));
        }
    }

    class EquippedTitleTooLongProblem extends ProfileProblem {
        public EquippedTitleTooLongProblem(String userId) {
            super(userId, "urn:quizup:profile:equippedTitleTooLong",
                    ProblemCategory.VALIDATION,
                    "Title too long",
                    "The equipped title must not exceed " + ProfileRules.MAX_EQUIPPED_TITLE_LENGTH + " characters",
                    null);
        }
    }
}
