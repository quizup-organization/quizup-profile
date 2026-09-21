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
import java.util.Objects;

/**
 * Journal des attributions d'XP par partie (idempotence de la progression).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "progression_awarded_game")
@IdClass(ProgressionAwardedGameEntity.AwardedGameId.class)
public class ProgressionAwardedGameEntity {

    @Id
    @Column(name = "user_id", length = 255, nullable = false)
    private String userId;

    @Id
    @Column(name = "game_id", length = 255, nullable = false)
    private String gameId;

    public ProgressionAwardedGameEntity(String userId, String gameId) {
        this.userId = userId;
        this.gameId = gameId;
    }

    /** Clé composite {@code (user_id, game_id)}. */
    @Getter
    @Setter
    public static class AwardedGameId implements Serializable {

        private String userId;
        private String gameId;

        public AwardedGameId() {
        }

        public AwardedGameId(String userId, String gameId) {
            this.userId = userId;
            this.gameId = gameId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AwardedGameId that)) {
                return false;
            }
            return Objects.equals(userId, that.userId) && Objects.equals(gameId, that.gameId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, gameId);
        }
    }
}
