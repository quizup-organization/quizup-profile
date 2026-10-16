package io.github.quizup.profile.domain.model;

import java.util.Arrays;
import java.util.List;

/**
 * Catalogue des **badges** : chaque badge appartient à une {@link BadgeFamily}, porte un
 * {@link BadgeTier} (bronze → argent → or → platine → émeraude → diamant) et se mesure sur une
 * {@link BadgeMetric} avec une cible ({@link #target()}) — ce qui permet d'afficher une
 * progression (« 7/10 duels », « plus que 3 ») et de laisser une raison de continuer après le
 * bronze, jusqu'au diamant.
 *
 * <p><b>Un palier = une constante</b> (et non un couple badge/palier) : les badges acquis restent
 * un simple ensemble de noms en base, sans migration, et l'attribution reste idempotente. Chaque
 * **échelle** (famille × métrique) monte ses six paliers ; les quatre exploits historiques
 * (`FIRST_WIN`, `PERFECT`, `LIGHTNING`, `STREAK_MASTER`) sont des **faits ponctuels** sans
 * progression.</p>
 *
 * <p><b>Portée</b> : les faits d'<i>exploit</i> (score, réponses rapides) comptent même en
 * entraînement ; les compteurs de volume, maîtrise, série, assiduité, exploration, précision,
 * rapidité et vision ne progressent que sur les duels entre joueurs, abonnements gagnés ou défis
 * lancés (cf. {@link BadgeRules}).</p>
 */
public enum Badge {

    // ── Exploit : faits ponctuels (sans progression intermédiaire) ─────────────────────────
    FIRST_WIN("Première victoire", BadgeFamily.EXPLOIT, BadgeTier.BRONZE, BadgeMetric.ONE_SHOT, 1),
    LIGHTNING("Éclair", BadgeFamily.EXPLOIT, BadgeTier.ARGENT, BadgeMetric.ONE_SHOT, 1),
    PERFECT("Perfectionniste", BadgeFamily.EXPLOIT, BadgeTier.OR, BadgeMetric.ONE_SHOT, 1),
    STREAK_MASTER("Série de feu", BadgeFamily.EXPLOIT, BadgeTier.PLATINE, BadgeMetric.ONE_SHOT, 1),

    // ── Volume : duels entre joueurs joués ────────────────────────────────────────────────
    DUELIST_BRONZE("Duelliste", BadgeFamily.VOLUME, BadgeTier.BRONZE, BadgeMetric.HUMAN_DUELS, 10),
    DUELIST_ARGENT("Bretteur", BadgeFamily.VOLUME, BadgeTier.ARGENT, BadgeMetric.HUMAN_DUELS, 50),
    DUELIST_OR("Lame d'élite", BadgeFamily.VOLUME, BadgeTier.OR, BadgeMetric.HUMAN_DUELS, 100),
    DUELIST_PLATINE("Légende des duels", BadgeFamily.VOLUME, BadgeTier.PLATINE, BadgeMetric.HUMAN_DUELS, 250),
    DUELIST_EMERAUDE("Maître d'armes", BadgeFamily.VOLUME, BadgeTier.EMERAUDE, BadgeMetric.HUMAN_DUELS, 500),
    DUELIST_DIAMANT("Mythe vivant", BadgeFamily.VOLUME, BadgeTier.DIAMANT, BadgeMetric.HUMAN_DUELS, 1000),

    // ── Victoires : duels entre joueurs gagnés ────────────────────────────────────────────
    WINNER_BRONZE("Vainqueur", BadgeFamily.VICTORY, BadgeTier.BRONZE, BadgeMetric.HUMAN_WINS, 10),
    WINNER_ARGENT("Triomphateur", BadgeFamily.VICTORY, BadgeTier.ARGENT, BadgeMetric.HUMAN_WINS, 50),
    WINNER_OR("Conquérant", BadgeFamily.VICTORY, BadgeTier.OR, BadgeMetric.HUMAN_WINS, 100),
    WINNER_PLATINE("Roi de l'arène", BadgeFamily.VICTORY, BadgeTier.PLATINE, BadgeMetric.HUMAN_WINS, 250),
    WINNER_EMERAUDE("Empereur du quiz", BadgeFamily.VICTORY, BadgeTier.EMERAUDE, BadgeMetric.HUMAN_WINS, 500),
    WINNER_DIAMANT("Légende absolue", BadgeFamily.VICTORY, BadgeTier.DIAMANT, BadgeMetric.HUMAN_WINS, 1000),

    // ── Maîtrise : XP cumulée dans un même sujet (alignée sur les grades) ────────────────
    MASTER_BRONZE("Spécialiste", BadgeFamily.MASTERY, BadgeTier.BRONZE, BadgeMetric.TOPIC_MASTERY,
            MasteryGrade.CONFIRME.xpThreshold()),
    MASTER_ARGENT("Érudit", BadgeFamily.MASTERY, BadgeTier.ARGENT, BadgeMetric.TOPIC_MASTERY,
            MasteryGrade.EXPERT.xpThreshold()),
    MASTER_OR("Autorité", BadgeFamily.MASTERY, BadgeTier.OR, BadgeMetric.TOPIC_MASTERY,
            MasteryGrade.MAITRE.xpThreshold()),
    MASTER_PLATINE("Sommité", BadgeFamily.MASTERY, BadgeTier.PLATINE, BadgeMetric.TOPIC_MASTERY,
            MasteryGrade.LEGENDE.xpThreshold()),
    MASTER_EMERAUDE("Oracle", BadgeFamily.MASTERY, BadgeTier.EMERAUDE, BadgeMetric.TOPIC_MASTERY, 60_000),
    MASTER_DIAMANT("Panthéon", BadgeFamily.MASTERY, BadgeTier.DIAMANT, BadgeMetric.TOPIC_MASTERY, 100_000),

    // ── Série : victoires consécutives ─────────────────────────────────────────────────────
    SERIES_BRONZE("En série", BadgeFamily.SERIES, BadgeTier.BRONZE, BadgeMetric.WIN_STREAK, 3),
    SERIES_ARGENT("Imbattable", BadgeFamily.SERIES, BadgeTier.ARGENT, BadgeMetric.WIN_STREAK, 10),
    SERIES_OR("Invincible", BadgeFamily.SERIES, BadgeTier.OR, BadgeMetric.WIN_STREAK, 25),
    SERIES_PLATINE("Feu sacré", BadgeFamily.SERIES, BadgeTier.PLATINE, BadgeMetric.WIN_STREAK, 50),
    SERIES_EMERAUDE("Mur d'acier", BadgeFamily.SERIES, BadgeTier.EMERAUDE, BadgeMetric.WIN_STREAK, 100),
    SERIES_DIAMANT("Forteresse", BadgeFamily.SERIES, BadgeTier.DIAMANT, BadgeMetric.WIN_STREAK, 200),

    // ── Assiduité : jours joués consécutifs ───────────────────────────────────────────────
    ASSIDUITY_BRONZE("Régulier", BadgeFamily.ASSIDUITY, BadgeTier.BRONZE, BadgeMetric.DAILY_STREAK, 3),
    ASSIDUITY_ARGENT("Accro", BadgeFamily.ASSIDUITY, BadgeTier.ARGENT, BadgeMetric.DAILY_STREAK, 7),
    ASSIDUITY_OR("Increvable", BadgeFamily.ASSIDUITY, BadgeTier.OR, BadgeMetric.DAILY_STREAK, 30),
    ASSIDUITY_PLATINE("Assidu", BadgeFamily.ASSIDUITY, BadgeTier.PLATINE, BadgeMetric.DAILY_STREAK, 60),
    ASSIDUITY_EMERAUDE("Inébranlable", BadgeFamily.ASSIDUITY, BadgeTier.EMERAUDE, BadgeMetric.DAILY_STREAK, 120),
    ASSIDUITY_DIAMANT("Immortel", BadgeFamily.ASSIDUITY, BadgeTier.DIAMANT, BadgeMetric.DAILY_STREAK, 365),

    // ── Précision : duels parfaits cumulés ────────────────────────────────────────────────
    PRECISION_BRONZE("Métronome", BadgeFamily.PRECISION, BadgeTier.BRONZE, BadgeMetric.PERFECT_DUELS, 10),
    PRECISION_ARGENT("Machine à points", BadgeFamily.PRECISION, BadgeTier.ARGENT, BadgeMetric.PERFECT_DUELS, 50),
    PRECISION_OR("Sans-faute absolu", BadgeFamily.PRECISION, BadgeTier.OR, BadgeMetric.PERFECT_DUELS, 100),
    PRECISION_PLATINE("Précision chirurgicale", BadgeFamily.PRECISION, BadgeTier.PLATINE,
            BadgeMetric.PERFECT_DUELS, 250),
    PRECISION_EMERAUDE("Impeccable", BadgeFamily.PRECISION, BadgeTier.EMERAUDE, BadgeMetric.PERFECT_DUELS, 500),
    PRECISION_DIAMANT("Infaillible", BadgeFamily.PRECISION, BadgeTier.DIAMANT, BadgeMetric.PERFECT_DUELS, 1000),

    // ── Rapidité : réponses rapides cumulées ──────────────────────────────────────────────
    SPEED_BRONZE("Doigt sur la gâchette", BadgeFamily.SPEED, BadgeTier.BRONZE, BadgeMetric.FAST_ANSWERS, 50),
    SPEED_ARGENT("Rafale", BadgeFamily.SPEED, BadgeTier.ARGENT, BadgeMetric.FAST_ANSWERS, 200),
    SPEED_OR("Réflexe absolu", BadgeFamily.SPEED, BadgeTier.OR, BadgeMetric.FAST_ANSWERS, 500),
    SPEED_PLATINE("Foudre", BadgeFamily.SPEED, BadgeTier.PLATINE, BadgeMetric.FAST_ANSWERS, 1_500),
    SPEED_EMERAUDE("Turbo", BadgeFamily.SPEED, BadgeTier.EMERAUDE, BadgeMetric.FAST_ANSWERS, 3_000),
    SPEED_DIAMANT("Vitesse lumière", BadgeFamily.SPEED, BadgeTier.DIAMANT, BadgeMetric.FAST_ANSWERS, 5_000),

    // ── Vision : questions illustrées (bonnes réponses cumulées) ─────────────────────────
    VISION_BRONZE("Observateur", BadgeFamily.VISION, BadgeTier.BRONZE, BadgeMetric.IMAGE_CORRECT, 15),
    VISION_ARGENT("Œil de lynx", BadgeFamily.VISION, BadgeTier.ARGENT, BadgeMetric.IMAGE_CORRECT, 100),
    VISION_OR("Vision absolue", BadgeFamily.VISION, BadgeTier.OR, BadgeMetric.IMAGE_CORRECT, 300),
    VISION_PLATINE("Mémoire photographique", BadgeFamily.VISION, BadgeTier.PLATINE,
            BadgeMetric.IMAGE_CORRECT, 750),
    VISION_EMERAUDE("Scanner", BadgeFamily.VISION, BadgeTier.EMERAUDE, BadgeMetric.IMAGE_CORRECT, 1_500),
    VISION_DIAMANT("Omniscient", BadgeFamily.VISION, BadgeTier.DIAMANT, BadgeMetric.IMAGE_CORRECT, 3_000),

    // ── Social : abonnés gagnés (cumulés) ────────────────────────────────────────────────
    SOCIAL_BRONZE("Populaire", BadgeFamily.SOCIAL, BadgeTier.BRONZE, BadgeMetric.FOLLOWERS_GAINED, 5),
    SOCIAL_ARGENT("Célébrité", BadgeFamily.SOCIAL, BadgeTier.ARGENT, BadgeMetric.FOLLOWERS_GAINED, 25),
    SOCIAL_OR("Icône", BadgeFamily.SOCIAL, BadgeTier.OR, BadgeMetric.FOLLOWERS_GAINED, 50),
    SOCIAL_PLATINE("Star", BadgeFamily.SOCIAL, BadgeTier.PLATINE, BadgeMetric.FOLLOWERS_GAINED, 100),
    SOCIAL_EMERAUDE("Superstar", BadgeFamily.SOCIAL, BadgeTier.EMERAUDE, BadgeMetric.FOLLOWERS_GAINED, 250),
    SOCIAL_DIAMANT("Monument", BadgeFamily.SOCIAL, BadgeTier.DIAMANT, BadgeMetric.FOLLOWERS_GAINED, 500),

    // ── Défis : défis nominatifs lancés ───────────────────────────────────────────────────
    CHALLENGES_BRONZE("Challenger", BadgeFamily.CHALLENGES, BadgeTier.BRONZE, BadgeMetric.CHALLENGES_SENT, 1),
    CHALLENGES_ARGENT("Provocateur", BadgeFamily.CHALLENGES, BadgeTier.ARGENT, BadgeMetric.CHALLENGES_SENT, 10),
    CHALLENGES_OR("Maître du défi", BadgeFamily.CHALLENGES, BadgeTier.OR, BadgeMetric.CHALLENGES_SENT, 50),
    CHALLENGES_PLATINE("Agitateur", BadgeFamily.CHALLENGES, BadgeTier.PLATINE,
            BadgeMetric.CHALLENGES_SENT, 100),
    CHALLENGES_EMERAUDE("Instigateur", BadgeFamily.CHALLENGES, BadgeTier.EMERAUDE,
            BadgeMetric.CHALLENGES_SENT, 250),
    CHALLENGES_DIAMANT("Meneur de jeu", BadgeFamily.CHALLENGES, BadgeTier.DIAMANT,
            BadgeMetric.CHALLENGES_SENT, 500),

    // ── Exploration : nombre de sujets joués (XP > 0) ──────────────────────────────────────
    EXPLORER_BRONZE("Curieux", BadgeFamily.EXPLORATION, BadgeTier.BRONZE, BadgeMetric.DISTINCT_TOPICS, 5),
    EXPLORER_ARGENT("Touche-à-tout", BadgeFamily.EXPLORATION, BadgeTier.ARGENT, BadgeMetric.DISTINCT_TOPICS, 10),
    EXPLORER_OR("Encyclopédie", BadgeFamily.EXPLORATION, BadgeTier.OR, BadgeMetric.DISTINCT_TOPICS, 20),
    EXPLORER_PLATINE("Globe-trotteur", BadgeFamily.EXPLORATION, BadgeTier.PLATINE,
            BadgeMetric.DISTINCT_TOPICS, 40),
    EXPLORER_EMERAUDE("Cartographe", BadgeFamily.EXPLORATION, BadgeTier.EMERAUDE,
            BadgeMetric.DISTINCT_TOPICS, 70),
    EXPLORER_DIAMANT("Atlas vivant", BadgeFamily.EXPLORATION, BadgeTier.DIAMANT,
            BadgeMetric.DISTINCT_TOPICS, 103),

    // ── Collection : nombre de badges obtenus (borné par le catalogue) ─────────────────────
    COLLECTOR_BRONZE("Collectionneur", BadgeFamily.COLLECTION, BadgeTier.BRONZE, BadgeMetric.EARNED_BADGES, 5),
    COLLECTOR_ARGENT("Amateur éclairé", BadgeFamily.COLLECTION, BadgeTier.ARGENT,
            BadgeMetric.EARNED_BADGES, 15),
    COLLECTOR_OR("Conservateur", BadgeFamily.COLLECTION, BadgeTier.OR, BadgeMetric.EARNED_BADGES, 30),
    COLLECTOR_PLATINE("Musée vivant", BadgeFamily.COLLECTION, BadgeTier.PLATINE,
            BadgeMetric.EARNED_BADGES, 45),
    COLLECTOR_EMERAUDE("Archiviste", BadgeFamily.COLLECTION, BadgeTier.EMERAUDE,
            BadgeMetric.EARNED_BADGES, 60),
    COLLECTOR_DIAMANT("Légende du musée", BadgeFamily.COLLECTION, BadgeTier.DIAMANT,
            BadgeMetric.EARNED_BADGES, 75);

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

    /** Badges d'une famille, dans l'ordre du palier (bronze → diamant). */
    public static List<Badge> ofFamily(BadgeFamily family) {
        return Arrays.stream(values())
                .filter(badge -> badge.family == family)
                .sorted((left, right) -> left.tier.compareTo(right.tier))
                .toList();
    }

    /** Badges d'une **échelle** (famille × métrique), dans l'ordre du palier. */
    public static List<Badge> ofLadder(BadgeFamily family, BadgeMetric metric) {
        return Arrays.stream(values())
                .filter(badge -> badge.family == family && badge.metric == metric)
                .sorted((left, right) -> left.tier.compareTo(right.tier))
                .toList();
    }
}
