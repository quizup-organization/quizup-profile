package io.github.quizup.profile.domain.model;

import java.util.Arrays;
import java.util.List;

/**
 * Catalogue des **badges** (exploits) : chaque badge appartient à une {@link BadgeFamily}, porte
 * un {@link BadgeTier} (bronze → argent → or) et se mesure sur une {@link BadgeMetric} avec une
 * cible ({@link #target()}) — ce qui permet d'afficher une progression (« 7/10 duels ») et de
 * laisser une raison de continuer après le bronze.
 *
 * <p><b>Un palier = une constante</b> (et non un couple badge/palier) : les badges acquis restent
 * un simple ensemble de noms en base, sans migration, et l'attribution reste idempotente.</p>
 *
 * <p><b>Règle produit</b> : les badges sont des faits de <b>duels entre joueurs</b> — un
 * entraînement contre bot ne les fait pas progresser (même règle que l'XP). La famille
 * <i>Assiduité</i> (série de jours) reste à brancher sur la projection d'activité, et la famille
 * <i>Social</i> attend les données d'abonnements : les deux sont volontairement hors de ce
 * catalogue pour l'instant.</p>
 */
public enum Badge {

    // ── Exploit : faits ponctuels d'un duel (sans progression intermédiaire) ────────────────
    FIRST_WIN("Première victoire", BadgeFamily.EXPLOIT, BadgeTier.BRONZE, BadgeMetric.ONE_SHOT, 1),
    LIGHTNING("Éclair", BadgeFamily.EXPLOIT, BadgeTier.ARGENT, BadgeMetric.ONE_SHOT, 1),
    PERFECT("Perfectionniste", BadgeFamily.EXPLOIT, BadgeTier.OR, BadgeMetric.ONE_SHOT, 1),
    STREAK_MASTER("Série de feu", BadgeFamily.EXPLOIT, BadgeTier.OR, BadgeMetric.ONE_SHOT, 1),

    // ── Volume : duels entre joueurs joués ──────────────────────────────────────────────────
    DUELIST_BRONZE("Duelliste", BadgeFamily.VOLUME, BadgeTier.BRONZE, BadgeMetric.HUMAN_DUELS, 10),
    DUELIST_ARGENT("Duelliste", BadgeFamily.VOLUME, BadgeTier.ARGENT, BadgeMetric.HUMAN_DUELS, 100),
    DUELIST_OR("Duelliste", BadgeFamily.VOLUME, BadgeTier.OR, BadgeMetric.HUMAN_DUELS, 500),

    // ── Volume : victoires entre joueurs ────────────────────────────────────────────────────
    WINNER_BRONZE("Vainqueur", BadgeFamily.VOLUME, BadgeTier.BRONZE, BadgeMetric.HUMAN_WINS, 10),
    WINNER_ARGENT("Vainqueur", BadgeFamily.VOLUME, BadgeTier.ARGENT, BadgeMetric.HUMAN_WINS, 100),
    WINNER_OR("Vainqueur", BadgeFamily.VOLUME, BadgeTier.OR, BadgeMetric.HUMAN_WINS, 500),

    // ── Maîtrise : grade atteint sur un sujet ───────────────────────────────────────────────
    MASTER_BRONZE("Spécialiste", BadgeFamily.MASTERY, BadgeTier.BRONZE, BadgeMetric.TOPIC_MASTERY,
            MasteryGrade.CONFIRME.xpThreshold()),
    MASTER_ARGENT("Spécialiste", BadgeFamily.MASTERY, BadgeTier.ARGENT, BadgeMetric.TOPIC_MASTERY,
            MasteryGrade.EXPERT.xpThreshold()),
    MASTER_OR("Spécialiste", BadgeFamily.MASTERY, BadgeTier.OR, BadgeMetric.TOPIC_MASTERY,
            MasteryGrade.LEGENDE.xpThreshold()),

    // ── Exploration : nombre de sujets joués (XP > 0) ──────────────────────────────────────
    EXPLORER_BRONZE("Curieux", BadgeFamily.EXPLORATION, BadgeTier.BRONZE, BadgeMetric.DISTINCT_TOPICS, 5),
    EXPLORER_ARGENT("Touche-à-tout", BadgeFamily.EXPLORATION, BadgeTier.ARGENT, BadgeMetric.DISTINCT_TOPICS, 25),
    EXPLORER_OR("Encyclopédie", BadgeFamily.EXPLORATION, BadgeTier.OR, BadgeMetric.DISTINCT_TOPICS, 50),

    // ── Collection : nombre de badges obtenus ───────────────────────────────────────────────
    // Cibles bornées par le catalogue actuel (16 badges) : atteignables aujourd'hui, relevées
    // quand les familles Assiduité et Social rejoindront le catalogue.
    COLLECTOR_BRONZE("Collectionneur", BadgeFamily.COLLECTION, BadgeTier.BRONZE, BadgeMetric.EARNED_BADGES, 5),
    COLLECTOR_ARGENT("Collectionneur", BadgeFamily.COLLECTION, BadgeTier.ARGENT, BadgeMetric.EARNED_BADGES, 10),
    COLLECTOR_OR("Collectionneur", BadgeFamily.COLLECTION, BadgeTier.OR, BadgeMetric.EARNED_BADGES, 15);

    private final String label;
    private final BadgeFamily family;
    private final BadgeTier tier;
    private final BadgeMetric metric;
    private final int target;

    Badge(String label, BadgeFamily family, BadgeTier tier, BadgeMetric metric, int target) {
        this.label = label;
        this.family = family;
        this.tier = tier;
        this.metric = metric;
        this.target = target;
    }

    /** Libellé du badge, **sans** le palier (« Duelliste »). */
    public String label() {
        return label;
    }

    public BadgeFamily family() {
        return family;
    }

    public BadgeTier tier() {
        return tier;
    }

    public BadgeMetric metric() {
        return metric;
    }

    /** Cible du compteur pour ce palier (`1` pour un fait ponctuel). */
    public int target() {
        return target;
    }

    /** Fait ponctuel : acquis d'un coup, aucune progression à afficher. */
    public boolean isOneShot() {
        return metric == BadgeMetric.ONE_SHOT;
    }

    /** Badges d'une famille, dans l'ordre du palier (bronze → or). */
    public static List<Badge> ofFamily(BadgeFamily family) {
        return Arrays.stream(values())
                .filter(badge -> badge.family == family)
                .sorted((left, right) -> left.tier.compareTo(right.tier))
                .toList();
    }
}
