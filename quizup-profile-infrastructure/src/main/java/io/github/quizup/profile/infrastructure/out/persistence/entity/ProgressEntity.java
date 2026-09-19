package io.github.quizup.profile.infrastructure.out.persistence.entity;

import io.github.quizup.profile.domain.model.Badge;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
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

    @Column(name = "xp_total", nullable = false)
    private int xpTotal;

    @Column(name = "level", nullable = false)
    private int level;

    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "games_played", nullable = false)
    private int gamesPlayed;

    @Column(name = "wins", nullable = false)
    private int wins;

    @Column(name = "losses", nullable = false)
    private int losses;

    @Column(name = "best_score", nullable = false)
    private int bestScore;

    @Column(name = "current_win_streak", nullable = false)
    private int currentWinStreak;

    @Column(name = "best_win_streak", nullable = false)
    private int bestWinStreak;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "progression_topic_entry", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "topic_id", length = 255)
    @Column(name = "xp", nullable = false)
    private Map<String, Integer> xpByTopic = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "progression_badge", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "badge", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Badge> badges = new HashSet<>();
}
