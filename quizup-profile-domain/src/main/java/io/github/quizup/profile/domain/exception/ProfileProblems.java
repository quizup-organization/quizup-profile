package io.github.quizup.profile.domain.exception;

import io.github.quizup.common.domain.exception.ProblemCategory;

import java.util.Map;

public interface ProfileProblems {

    class MissingProfileIdProblem extends ProfileProblem {
        public MissingProfileIdProblem(String profileId) {
            super(profileId, "urn:quizup:profile:missingProfileId",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Profile ID required",
                    "A profileId is required to handle profile commands",
                    null);
        }
    }

    class ProfileNotFoundProblem extends ProfileProblem {
        public ProfileNotFoundProblem(String profileId) {
            super(profileId, "urn:quizup:profile:notFound",
                    ProblemCategory.BUSINESS_RESOURCE_MISSING,
                    "Profile not found",
                    "The profile " + profileId + " was not found",
                    null);
        }
    }

    class MissingTopicIdProblem extends ProfileProblem {
        public MissingTopicIdProblem(String profileId) {
            super(profileId, "urn:quizup:profile:missingTopicId",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Topic ID required",
                    "A topicId is required to record a match result on profile " + profileId,
                    null);
        }
    }

    class MissingGameResultProblem extends ProfileProblem {
        public MissingGameResultProblem(String profileId) {
            super(profileId, "urn:quizup:profile:missingGameResult",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Game result required",
                    "A game result is required to update profile " + profileId,
                    null);
        }
    }

    class MissingGameIdProblem extends ProfileProblem {
        public MissingGameIdProblem(String profileId) {
            super(profileId, "urn:quizup:profile:missingGameId",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Game ID required",
                    "A gameId is required to record a match result on profile " + profileId,
                    null);
        }
    }

    class InvalidGameScoreProblem extends ProfileProblem {
        public InvalidGameScoreProblem(String profileId, int score) {
            super(profileId, "urn:quizup:profile:invalidGameScore",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Invalid game score",
                    "The score must be greater than or equal to 0",
                    Map.of("score", score));
        }
    }
}

