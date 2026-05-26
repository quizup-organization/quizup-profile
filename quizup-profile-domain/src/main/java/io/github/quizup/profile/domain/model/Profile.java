package io.github.quizup.profile.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Builder(toBuilder = true)
public record Profile(
        String profileId,
        int totalExperience,
        int wins,
        int losses,
        int draws,
        int winStreak,
        int lossStreak,
        int drawStreak,
        Map<String, ProfileTopic> topics,
        List<ProfileGame> games,
        Instant createdAt,
        Instant updatedAt
) implements Statistics {

    public static Profile empty(String profileId, Instant now) {
        return new Profile(
                profileId,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                Map.of(),
                List.of(),
                now,
                now
        );
    }
}

