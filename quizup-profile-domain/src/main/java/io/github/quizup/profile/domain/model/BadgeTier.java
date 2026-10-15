package io.github.quizup.profile.domain.model;

/**
 * Palier d'un {@link Badge} au sein de sa famille : bronze → argent → or → platine → émeraude →
 * diamant. Un badge déjà acquis laisse donc une raison de continuer (le palier suivant), et le
 * métal donne la rareté à l'affichage (collection, célébration de déblocage).
 */
public enum BadgeTier {

    BRONZE,
    ARGENT,
    OR,
    PLATINE,
    EMERAUDE,
    DIAMANT;

    public boolean isHighest() {
        return this == DIAMANT;
    }
}
