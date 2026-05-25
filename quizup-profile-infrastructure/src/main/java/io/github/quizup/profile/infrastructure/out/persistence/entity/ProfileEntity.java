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
        @Index(name = "idx_profile_total_xp", columnList = "global_total_experience"),
        @Index(name = "idx_profile_wins", columnList = "global_wins")
})
public class ProfileEntity {

    @Id
    @Searchable(type = FieldType.STRING)
    @Column(name = "profile_id", nullable = false, length = 255)
    private String profileId;

    @Column(name = "win_streak", nullable = false)
    private int winStreak;

    @Column(name = "loss_streak", nullable = false)
    private int lossStreak;

    @Column(name = "draw_streak", nullable = false)
    private int drawStreak;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "global_total_experience", nullable = false)
    private int globalTotalExperience;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "global_wins", nullable = false)
    private int globalWins;

    @Column(name = "global_losses", nullable = false)
    private int globalLosses;

    @Column(name = "global_draws", nullable = false)
    private int globalDraws;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_topic_statistics_entry", joinColumns = @JoinColumn(name = "profile_id"))
    @MapKeyColumn(name = "topic_id")
    private Map<String, TopicStatisticsEmbeddable> topicStatistics = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_badge_entry", joinColumns = @JoinColumn(name = "profile_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "badge_type")
    private Map<BadgeType, BadgeEmbeddable> badges = new EnumMap<>(BadgeType.class);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_game_result_entry", joinColumns = @JoinColumn(name = "profile_id"))
    @OrderColumn(name = "position_idx")
    private List<GameResultEmbeddable> recentGameResults = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

