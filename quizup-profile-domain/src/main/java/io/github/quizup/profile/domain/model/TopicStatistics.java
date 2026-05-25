package io.github.quizup.profile.domain.model;


public record TopicStatistics(
        String topicId,
        int totalExperience,
        int wins,
        int losses,
        int draws
) implements Statistics {

    public static TopicStatistics empty(String topicId) {
        return new TopicStatistics(
                topicId,
                0,
                0,
                0,
                0
        );
    }

}
