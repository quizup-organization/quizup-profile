package io.github.quizup.profile.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Règles **pures** d'attribution des badges : à partir des **faits d'un duel** et des
 * **compteurs déjà mis à jour** par ce duel, retourne les badges **nouvellement** acquis —
 * jamais ceux déjà possédés, et jamais de « retrait ».
 *
 * <p>L'évaluation est **générique par métrique** : un badge de {@link BadgeMetric#HUMAN_DUELS},
 * {@code HUMAN_WINS} ou {@code TOPIC_MASTERY} est acquis dès que le compteur atteint sa
 * {@link Badge#target() cible} — ajouter un palier au catalogue suffit donc à l'activer, sans
 * toucher à l'agrégat.</p>
 *
 * <p><b>Portée</b> : les faits d'<i>exploit</i> sont des faits du duel et comptent même en
 * entraînement contre bot ; les familles de volume et de maîtrise, elles, ne progressent que
 * dans les **duels entre joueurs** (les compteurs ne sont incrémentés qu'à ces occasions).</p>
 *
 * <p>La famille {@code COLLECTION} se débloque <b>en cascade</b> : un badge acquis peut faire
 * franchir un palier de collection, qui peut lui-même en faire franchir un autre.</p>
 */
public final class BadgeRules {

    /** Bonnes réponses rapides (< 3 s) dans un même duel pour « Éclair ». */
    public static final int LIGHTNING_FAST_ANSWERS = 5;

    /** Victoires consécutives dans un même thème pour « Série de feu ». */
    public static final int STREAK_MASTER_WINS = 10;

    private BadgeRules() {
    }

    /**
     * Faits d'un duel et compteurs du joueur **après** prise en compte de ce duel.
     *
     * @param humanDuels      duels entre joueurs joués (hors entraînement)
     * @param humanWins       duels entre joueurs gagnés (hors entraînement)
     * @param topicXp         XP cumulée dans le sujet du duel
     * @param winStreakInTopic victoires consécutives courantes dans ce sujet
     */
    public record DuelFacts(
            int gameScore,
            boolean won,
            int fastAnswers,
            int humanDuels,
            int humanWins,
            int topicXp,
            int winStreakInTopic,
            /** Nombre de sujets joués (XP > 0), entraînement exclu. */
            int distinctTopics,
            /** Duels parfaits cumulés (entraînement compris : fait d'exploit du duel). */
            int perfectDuels,
            /** Réponses rapides cumulées (entraînement compris : fait d'exploit du duel). */
            int fastAnswersTotal,
            /** Meilleure série de jours joués consécutifs, au moment du duel. */
            int dayStreak
    ) {
    }

    /**
     * Compteurs **hors duel** d'un joueur (alimentés par les autres services : abonnements,
     * défis nominatifs), à jour au moment de l'évaluation.
     */
    public record CounterFacts(
            /** Abonnés gagnés, cumulés. */
            int followersGained,
            /** Défis nominatifs lancés. */
            int challengesSent
    ) {
    }

    /**
     * Badges nouvellement acquis par un **compteur hors duel** (abonné gagné, défi lancé).
     *
     * @param facts         compteurs à jour
     * @param alreadyEarned badges déjà possédés
     * @return les seuls badges à émettre (ordre stable, par ordinal)
     */
    public static Set<Badge> evaluateCounters(CounterFacts facts, Set<Badge> alreadyEarned) {
        Set<Badge> owned = EnumSet.noneOf(Badge.class);
        owned.addAll(alreadyEarned);

        collectReached(owned, BadgeMetric.FOLLOWERS_GAINED, facts.followersGained());
        collectReached(owned, BadgeMetric.CHALLENGES_SENT, facts.challengesSent());
        cascadeCollection(owned);

        owned.removeAll(alreadyEarned);
        return owned;
    }

    /**
     * Badges nouvellement acquis à l'issue du duel.
     *
     * @param facts        faits du duel et compteurs à jour
     * @param alreadyEarned badges déjà possédés par le joueur
     * @return les seuls badges à émettre (ordre stable, par ordinal)
     */
    public static Set<Badge> evaluate(DuelFacts facts, Set<Badge> alreadyEarned) {
        Set<Badge> owned = EnumSet.noneOf(Badge.class);
        owned.addAll(alreadyEarned);

        collectExploits(owned, facts);
        collectReached(owned, BadgeMetric.HUMAN_DUELS, facts.humanDuels());
        collectReached(owned, BadgeMetric.HUMAN_WINS, facts.humanWins());
        collectReached(owned, BadgeMetric.TOPIC_MASTERY, facts.topicXp());
        collectReached(owned, BadgeMetric.DISTINCT_TOPICS, facts.distinctTopics());
        collectReached(owned, BadgeMetric.WIN_STREAK, facts.winStreakInTopic());
        collectReached(owned, BadgeMetric.PERFECT_DUELS, facts.perfectDuels());
        collectReached(owned, BadgeMetric.FAST_ANSWERS, facts.fastAnswersTotal());
        collectReached(owned, BadgeMetric.DAILY_STREAK, facts.dayStreak());

        cascadeCollection(owned);

        owned.removeAll(alreadyEarned);
        return owned;
    }

    /** Collection : le total possédé grandit à chaque badge ajouté → boucle jusqu'à stabilité. */
    private static void cascadeCollection(Set<Badge> owned) {
        boolean changed = true;
        while (changed) {
            changed = collectReached(owned, BadgeMetric.EARNED_BADGES, owned.size());
        }
    }

    private static void collectExploits(Set<Badge> owned, DuelFacts facts) {
        if (facts.won()) {
            owned.add(Badge.FIRST_WIN);
        }
        if (facts.gameScore() >= ProgressionRules.PERFECT_SCORE) {
            owned.add(Badge.PERFECT);
        }
        if (facts.fastAnswers() >= LIGHTNING_FAST_ANSWERS) {
            owned.add(Badge.LIGHTNING);
        }
        if (facts.winStreakInTopic() >= STREAK_MASTER_WINS) {
            owned.add(Badge.STREAK_MASTER);
        }
    }

    /**
     * Ajoute les badges d'une métrique dont la cible est atteinte par {@code value}.
     *
     * @return vrai si au moins un badge a été ajouté
     */
    private static boolean collectReached(Set<Badge> owned, BadgeMetric metric, int value) {
        boolean added = false;
        for (Badge badge : Badge.values()) {
            if (badge.metric() == metric && badge.target() <= value) {
                added |= owned.add(badge);
            }
        }

        return added;
    }
}
