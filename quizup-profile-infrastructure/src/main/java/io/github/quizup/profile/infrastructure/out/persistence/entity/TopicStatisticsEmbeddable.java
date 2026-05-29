package io.github.quizup.profile.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Embeddable
public class TopicStatisticsEmbeddable {

    @Column(name = "total_experience", nullable = false)
    private int totalExperience;

    @Column(name = "level", nullable = false)
    private int level;

    @Column(name = "wins", nullable = false)
    private int wins;

    @Column(name = "losses", nullable = false)
    private int losses;

    @Column(name = "draws", nullable = false)
    private int draws;

    @Column(name = "win_streak", nullable = false)
    private int winStreak;

    @Column(name = "loss_streak", nullable = false)
    private int lossStreak;

    @Column(name = "draw_streak", nullable = false)
    private int drawStreak;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
