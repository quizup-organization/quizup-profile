package io.github.quizup.profile.domain.exception;

import io.github.quizup.microservice.core.domain.exception.ProblemCategory;
import io.github.quizup.microservice.core.domain.exception.BaseProblem;

import java.util.Map;

public interface ProgressionProblems {

    class ProgressionNotFoundProblem extends BaseProblem {
        public ProgressionNotFoundProblem(String userId) {
            super("urn:quizup:profile:progressionNotFound",
                    ProblemCategory.BUSINESS_RESOURCE_MISSING,
                    "Progression not found",
                    "No progression was found for user " + userId,
                    Map.of("userId", userId));
        }
    }
}
