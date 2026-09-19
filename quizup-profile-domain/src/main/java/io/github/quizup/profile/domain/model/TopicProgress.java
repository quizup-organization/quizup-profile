package io.github.quizup.profile.domain.model;

/**
 * Progression d'un joueur dans un thème donné : XP cumulée et niveau dérivé.
 * L'XP est séparée par thème (spécification produit).
 */
public record TopicProgress(
        String topicId,
        int xp,
        int level
) {
    public static TopicProgress of(String topicId, int xp) {
        return new TopicProgress(topicId, xp, ProgressionRules.levelFor(xp));
    }
}
