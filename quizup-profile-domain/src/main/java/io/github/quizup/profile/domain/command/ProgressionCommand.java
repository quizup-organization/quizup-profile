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
}
