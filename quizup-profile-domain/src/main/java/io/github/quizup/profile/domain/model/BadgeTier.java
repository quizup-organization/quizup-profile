package io.github.quizup.profile.domain.model;

/**
 * Palier d'un {@link Badge} au sein de sa famille : fer → bronze → argent → or → platine →
 * émeraude → diamant. Un badge déjà acquis laisse donc une raison de continuer (le palier
 * suivant), et le métal donne la rareté à l'affichage (collection, célébration de déblocage).
 *
 * <p>L'échelle est alignée sur celle des rangs de titre ({@link TitleRank}) : même vocabulaire
 * de rareté, mais les deux concepts restent distincts côté domaine.</p>
 */
public enum BadgeTier {

    FER,
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
