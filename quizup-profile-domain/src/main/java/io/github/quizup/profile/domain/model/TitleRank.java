package io.github.quizup.profile.domain.model;

/**
 * Rang de rareté d'un **titre thématique** : fer → bronze → argent → or → platine → émeraude →
 * diamant.
 *
 * <p>Le rang n'est **pas persisté** : il est dérivé du {@link MasteryGrade} porté par le titre
 * ({@link MasteryGrade#rank()}). La clé persistée reste {@code topicId:GRADE}, ce qui permet de
 * faire évoluer la correspondance grade → rang sans migration.</p>
 *
 * <p>L'échelle est volontairement identique à celle des paliers de badges
 * ({@link BadgeTier}) : les deux expriment la même rareté, à des niveaux différents du produit.</p>
 */
public enum TitleRank {

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
