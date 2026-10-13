package io.github.quizup.profile.domain.model;

/**
 * Progression d'un joueur dans un thème donné : XP cumulée, niveau dérivé et parties humaines
 * jouées. L'XP est séparée par thème (spécification produit) ; les duels bot comptent dans
 * l'XP mais pas dans les parties (même règle que les stats V/N/D).
 */
public record TopicProgress(
        String topicId,
        int xp,
        int level,
        int games
) {
    public static TopicProgress of(String topicId, int xp, int games) {
        return new TopicProgress(topicId, xp, ProgressionRules.levelFor(xp), games);
    }
}
