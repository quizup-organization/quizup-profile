package io.github.quizup.profile.domain.exception;

import io.github.quizup.common.domain.exception.ProblemCategory;

import java.util.Map;

public interface ProfileProblems {

    class MissingUserIdProblem extends ProfileProblem {
        public MissingUserIdProblem(String profileId) {
            super(profileId, "urn:quizup:profile:missingUserId",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "User ID required",
                    "A userId is required to create profile " + profileId,
                    null);
        }
    }

    class MissingPseudonymProblem extends ProfileProblem {
        public MissingPseudonymProblem(String profileId) {
            super(profileId, "urn:quizup:profile:missingPseudonym",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Pseudonym required",
                    "A pseudonym is required to create profile " + profileId,
                    null);
        }
    }

    class InvalidPseudonymProblem extends ProfileProblem {
        public InvalidPseudonymProblem(String profileId, String pseudonym) {
            super(profileId, "urn:quizup:profile:invalidPseudonym",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Invalid pseudonym",
                    "Pseudonym must contain between 3 and 20 characters",
                    Map.of("pseudonym", pseudonym == null ? "" : pseudonym));
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

    class MissingMatchResultProblem extends ProfileProblem {
        public MissingMatchResultProblem(String profileId) {
            super(profileId, "urn:quizup:profile:missingMatchResult",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Match result required",
                    "A match result is required to update profile " + profileId,
                    null);
        }
    }
}

