package io.github.quizup.profile.infrastructure.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Embeddable
public class TopicStatisticsEmbeddable {

    @Column(name = "total_experience", nullable = false)
    private int totalExperience;

    @Column(name = "wins", nullable = false)
    private int wins;

    @Column(name = "losses", nullable = false)
    private int losses;

    @Column(name = "draws", nullable = false)
    private int draws;
}

