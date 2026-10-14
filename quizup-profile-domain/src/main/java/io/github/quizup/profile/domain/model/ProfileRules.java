package io.github.quizup.profile.domain.model;

import io.github.quizup.microservice.core.domain.model.i18n.Language;

/**
 * Règles de validation du domaine Profile (limites de longueur).
 */
public final class ProfileRules {

    public static final int MAX_PSEUDONYM_LENGTH = 100;

    public static final int MAX_BIO_LENGTH = 300;

    public static final int MAX_COUNTRY_LENGTH = 100;

    /** Sérialisation JSON des options d'avatar DiceBear (styles micah, lorelei, notionists). */
    public static final int MAX_AVATAR_OPTIONS_LENGTH = 2000;

    /** URL externe de la bannière de profil (image panoramique, http/https). */
    public static final int MAX_BANNER_URL_LENGTH = 1024;

    /** Clé du titre porté (`topicId:GRADE`, cf. {@link PlayerTitle}). */
    public static final int MAX_EQUIPPED_TITLE_LENGTH = 128;

    /** Langue par défaut d'un profil (appliquée à la création). */
    public static final Language DEFAULT_LANGUAGE = Language.FR;

    private ProfileRules() {
    }
}
