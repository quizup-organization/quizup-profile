package io.github.quizup.profile.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Journal des parties comptées par jour (idempotence du compteur d'activité).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "progression_activity_day_game")
@IdClass(ActivityDayGameEntity.ActivityDayGameId.class)
public class ActivityDayGameEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Id
    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Id
    @Column(name = "game_id", nullable = false)
    private String gameId;

    public ActivityDayGameEntity(String userId, LocalDate activityDate, String gameId) {
        this.userId = userId;
        this.activityDate = activityDate;
        this.gameId = gameId;
    }

    /** Clé composite {@code (user_id, activity_date, game_id)}. */
    @Getter
    @Setter
    public static class ActivityDayGameId implements Serializable {

        private String userId;
        private LocalDate activityDate;
        private String gameId;

        public ActivityDayGameId() {
        }

        public ActivityDayGameId(String userId, LocalDate activityDate, String gameId) {
            this.userId = userId;
            this.activityDate = activityDate;
            this.gameId = gameId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ActivityDayGameId that)) {
                return false;
            }
            return Objects.equals(userId, that.userId)
                    && Objects.equals(activityDate, that.activityDate)
                    && Objects.equals(gameId, that.gameId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, activityDate, gameId);
        }
    }
}
