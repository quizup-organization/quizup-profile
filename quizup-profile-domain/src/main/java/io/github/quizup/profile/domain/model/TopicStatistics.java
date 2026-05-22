package io.github.quizup.profile.domain.model;

public record TopicStatistics(
        String topicId,
        int totalExperience,
        int wins,
        int losses,
        int draws
) implements Statistics {

}
