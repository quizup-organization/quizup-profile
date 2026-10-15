package io.github.quizup.profile.domain.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public interface ProgressionCommand {

    /**
     * Attribue l'XP d'un duel à un joueur. Idempotente : un même {@code gameId}
     * ne peut être crédité qu'une fois par joueur.
     *
     * <p>{@code progressId} est l'identifiant de l'agrégat de progression
     * (namespacé, cf. {@code ProgressionRules.progressIdFor}), distinct du
     * {@code userId} qui identifie le {@code ProfileAggregate}.</p>
     *
     * <p>{@code draw} distingue l'égalité de la défaite ; {@code botGame} signale un
     * <b>entraînement</b> : aucune XP n'est attribuée (la progression ne vient que des duels
     * entre joueurs), les badges restent évalués, et les consommateurs (stats V/N/D,
     * classement) excluent ces duels.</p>
     */
    record AwardXpCommand(
            @TargetAggregateIdentifier String progressId,
            String userId,
            String gameId,
            String topicId,
            int gameScore,
            boolean won,
            boolean draw,
            boolean botGame,
            int correctAnswers,
            int fastAnswers
    ) implements ProgressionCommand {
    }

    /**
     * Évalue les **badges** d'un joueur à l'issue d'un duel (agrégat dédié
     * {@code badges:<userId>}). Idempotente : un même {@code gameId} n'est évalué qu'une fois.
     *
     * <p>Les faits du duel sont ceux de {@code XpAwardedEvent} ; l'agrégat porte lui-même les
     * compteurs nécessaires (duels/victoires entre joueurs, séries par thème, XP par thème).</p>
     */
    record EvaluateBadgesCommand(
            @TargetAggregateIdentifier String badgeId,
            String userId,
            String gameId,
            String topicId,
            int xp,
            int gameScore,
            boolean won,
            boolean botGame,
            int fastAnswers,
            /** Jour local du duel (fuseau d'activité), préparé par la saga pour l'assiduité. */
            java.time.LocalDate activeDay
    ) implements ProgressionCommand {
    }
}
