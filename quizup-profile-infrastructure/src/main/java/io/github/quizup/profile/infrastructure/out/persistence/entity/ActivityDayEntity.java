package io.github.quizup.profile.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Entité JPA du nombre de parties jouées un jour donné (point du graphe d'activité).
 */
@Getter
@Setter
@Entity
@Table(name = "progression_activity_day")
@IdClass(ActivityDayEntity.ActivityDayId.class)
public class ActivityDayEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Id
    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "games_count", nullable = false)
    private int gamesCount;

    /** Clé composite {@code (user_id, activity_date)}. */
    @Getter
    @Setter
    public static class ActivityDayId implements Serializable {

        private String userId;
        private LocalDate activityDate;

        public ActivityDayId() {
        }

        public ActivityDayId(String userId, LocalDate activityDate) {
            this.userId = userId;
            this.activityDate = activityDate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ActivityDayId that)) {
                return false;
            }
            return Objects.equals(userId, that.userId) && Objects.equals(activityDate, that.activityDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, activityDate);
        }
    }
}
