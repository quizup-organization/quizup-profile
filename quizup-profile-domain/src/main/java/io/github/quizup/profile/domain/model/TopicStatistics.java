package io.github.quizup.profile.domain.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record TopicStatistics(
        String topicId,
        int totalExperience,
        int wins,
        int losses,
        int draws
) implements Statistics {

    public static TopicStatistics empty(String topicId) {
        return new TopicStatistics(topicId, 0, 0, 0, 0);
    }

    public TopicStatistics addGame(int xpEarned, GameResultType result) {
        return switch (result) {
            case WIN -> new TopicStatistics(topicId, totalExperience + xpEarned, wins + 1, losses, draws);
            case LOSS -> new TopicStatistics(topicId, totalExperience + xpEarned, wins, losses + 1, draws);
            case DRAW -> new TopicStatistics(topicId, totalExperience + xpEarned, wins, losses, draws + 1);
        };
    }

}
