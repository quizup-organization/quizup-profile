package io.github.quizup.profile.infrastructure.out.persistence.entity;

import io.github.quizup.profile.domain.model.Badge;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * ProgressEntity — projection JPA de la progression d'un joueur.
 * Table read-only mise à jour via les Event Handlers.
 */
@Setter
@Getter
@Entity
@Table(name = "progression_entry")
public class ProgressEntity {

    @Id
    @Column(name = "user_id", length = 255, nullable = false)
    private String userId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "games_played", nullable = false)
    private int gamesPlayed;

    @Column(name = "wins", nullable = false)
    private int wins;

    @Column(name = "losses", nullable = false)
    private int losses;

    @Column(name = "draws", nullable = false)
    private int draws;

    @Column(name = "best_score", nullable = false)
    private int bestScore;

    @Column(name = "current_win_streak", nullable = false)
    private int currentWinStreak;

    @Column(name = "best_win_streak", nullable = false)
    private int bestWinStreak;

    /** Duels parfaits cumulés (faits d'exploit, entraînement compris) — badge Précision. */
    @Column(name = "perfect_duels", nullable = false)
    private int perfectDuels;

    /** Réponses rapides cumulées (faits d'exploit, entraînement compris) — badge Rapidité. */
    @Column(name = "fast_answers_total", nullable = false)
    private int fastAnswersTotal;

    /** Bonnes réponses cumulées sur les questions illustrées — badge Vision. */
    @Column(name = "image_correct_total", nullable = false)
    private int imageCorrectTotal;

    /** Abonnés gagnés (cumulés) — badge Social. */
    @Column(name = "followers_gained", nullable = false)
    private int followersGained;

    /** Défis nominatifs lancés — badge Défis. */
    @Column(name = "challenges_sent", nullable = false)
    private int challengesSent;

    /** Série courante de jours joués — badge Assiduité (même règle que la projection d'activité). */
    @Column(name = "current_day_streak", nullable = false)
    private int currentDayStreak;

    /** Meilleure série de jours joués (monotone) — progression affichée du badge Assiduité. */
    @Column(name = "best_day_streak", nullable = false)
    private int bestDayStreak;

    /** Dernier jour joué (fuseau d'activité) : idempotence de la série. */
    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;

    /** Elo global courant (profil, matchmaking, bordure). */
    @Column(name = "rating", nullable = false)
    private int rating;

    /** Meilleur Elo atteint. */
    @Column(name = "peak_rating", nullable = false)
    private int peakRating;

    /** Nombre de duels classés (hors bot, hors plafond anti-farm). */
    @Column(name = "rated_games", nullable = false)
    private int ratedGames;

    @Column(name = "provisional", nullable = false)
    private boolean provisional;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "progression_topic_entry", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "topic_id", length = 255)
    @Column(name = "xp", nullable = false)
    private Map<String, Integer> xpByTopic = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "progression_topic_games", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "topic_id", length = 255)
    @Column(name = "games", nullable = false)
    private Map<String, Integer> gamesByTopic = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "progression_badge", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "badge", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Badge> badges = new HashSet<>();
}
