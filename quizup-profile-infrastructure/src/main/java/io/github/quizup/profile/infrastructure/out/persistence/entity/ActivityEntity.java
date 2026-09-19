package io.github.quizup.profile.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Entité JPA de l'activité journalière d'un joueur (série courante/record, dernier jour actif).
 */
@Getter
@Setter
@Entity
@Table(name = "progression_activity")
public class ActivityEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak;

    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;
}
