package io.github.quizup.profile.domain.model;

/**
 * Palier d'un {@link Badge} au sein de sa famille : bronze → argent → or. Un badge déjà acquis
 * laisse donc une raison de continuer (le palier suivant).
 */
public enum BadgeTier {

    BRONZE,
    ARGENT,
    OR;

    public boolean isHighest() {
        return this == OR;
    }
}
