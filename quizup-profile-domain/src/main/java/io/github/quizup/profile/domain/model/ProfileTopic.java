package io.github.quizup.profile.domain.model;


import java.util.Collections;
import java.util.List;

public record ProfileTopic(
        String topicId,
        int totalExperience,
        int wins,
        int losses,
        int draws,
        List<ProfileGame> games,
        int winStreak,
        int drawStreak,
        int lossStreak
) implements Statistics {

    public static ProfileTopic empty(String topicId) {
        return new ProfileTopic(
                topicId,
                0,
                0,
                0,
                0,
                Collections.emptyList(),
                0,
                0,
                0
        );
    }

}
