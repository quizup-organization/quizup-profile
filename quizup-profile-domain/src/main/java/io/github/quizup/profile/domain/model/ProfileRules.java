package io.github.quizup.profile.domain.model;

/**
 * Règles de validation du domaine Profile (limites de longueur).
 */
public final class ProfileRules {

    public static final int MAX_DISPLAY_NAME_LENGTH = 100;

    public static final int MAX_BIO_LENGTH = 300;

    public static final int MAX_COUNTRY_LENGTH = 100;

    private ProfileRules() {
    }
}
