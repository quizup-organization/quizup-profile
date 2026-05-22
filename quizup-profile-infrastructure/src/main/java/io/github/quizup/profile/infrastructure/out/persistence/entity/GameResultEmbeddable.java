package io.github.quizup.profile.infrastructure.out.persistence.entity;

import io.github.quizup.profile.domain.model.GameResultType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Embeddable
public class GameResultEmbeddable {

    @Column(name = "game_id", nullable = false, length = 255)
    private String gameId;

    @Column(name = "topic_id", nullable = false, length = 255)
    private String topicId;

    @Column(name = "topic_name", nullable = false, length = 255)
    private String topicName;

    @Column(name = "opponent_id", nullable = false, length = 255)
    private String opponentId;

    @Column(name = "opponent_name", nullable = false, length = 255)
    private String opponentName;

    @Column(name = "player_score", nullable = false)
    private int playerScore;

    @Column(name = "opponent_score", nullable = false)
    private int opponentScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", nullable = false, length = 20)
    private GameResultType result;

    @Column(name = "played_at", nullable = false)
    private Instant playedAt;
}

