package io.github.quizup.profile.infrastructure.out.persistence.entity;

import io.github.quizup.common.domain.model.search.FieldType;
import io.github.quizup.common.domain.model.search.Searchable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.*;

@Setter
@Getter
@Entity
@Table(name = "profile_entry", indexes = {
        @Index(name = "idx_profile_total_xp", columnList = "total_experience"),
        @Index(name = "idx_profile_level", columnList = "level"),
        @Index(name = "idx_profile_wins", columnList = "wins")
})
public class ProfileEntity {

    @Id
    @Searchable(type = FieldType.STRING)
    @Column(name = "profile_id", nullable = false, length = 255)
    private String profileId;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "total_experience", nullable = false)
    private int totalExperience;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "level", nullable = false)
    private int level;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "wins", nullable = false)
    private int wins;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "losses", nullable = false)
    private int losses;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "draws", nullable = false)
    private int draws;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "win_streak", nullable = false)
    private int winStreak;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "loss_streak", nullable = false)
    private int lossStreak;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "draw_streak", nullable = false)
    private int drawStreak;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_topic_statistics_entry", joinColumns = @JoinColumn(name = "profile_id"))
    @MapKeyColumn(name = "topic_id")
    private Map<String, TopicStatisticsEmbeddable> topics = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_game_entry", joinColumns = @JoinColumn(name = "profile_id"))
    @OrderColumn(name = "position_idx")
    private List<GameResultEmbeddable> games = new ArrayList<>();

    @Searchable(type = FieldType.DATE)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Searchable(type = FieldType.DATE)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

