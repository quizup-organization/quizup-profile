package io.github.quizup.profile.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Builder(toBuilder = true)
public record ProfileTopic(
        String topicId,
        int totalExperience,
        int level,
        int wins,
        int losses,
        int draws,
        List<ProfileGame> games,
        int winStreak,
        int drawStreak,
        int lossStreak,
        Instant createdAt,
        Instant updatedAt
) implements Statistics {

    @Override
    public int level() {
        return level;
    }

    public static ProfileTopic empty(String topicId, Instant now) {
        return new ProfileTopic(
                topicId,
                0,
                0,
                0,
                0,
                0,
                Collections.emptyList(),
                0,
                0,
                0,
                now,
                now
        );
    }
}
