package io.github.quizup.profile.domain.exception;

import io.github.quizup.microservice.core.domain.exception.BaseProblem;
import io.github.quizup.microservice.core.domain.exception.ProblemCategory;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe de base pour toutes les exceptions métier liées au domaine Profile
 */
@Getter
public abstract class ProfileProblem extends BaseProblem {

    private final String userId;

    protected ProfileProblem(
            String userId,
            String type,
            ProblemCategory category,
            String title,
            String detail,
            Map<String, Object> context) {
        super(
                type,
                category,
                title,
                detail,
                mergeContext(context, userId)
        );
        this.userId = userId;
    }

    protected ProfileProblem(
            String userId,
            String type,
            String title,
            String detail,
            Map<String, Object> context) {
        this(userId, type, ProblemCategory.BUSINESS_INVALID_COMMAND, title, detail, context);
    }

    protected ProfileProblem(
            String userId,
            String type,
            String title,
            String detail) {
        this(userId, type, ProblemCategory.BUSINESS_INVALID_COMMAND, title, detail, null);
    }

    protected ProfileProblem(
            String userId,
            String type,
            String title) {
        this(userId, type, title, null, null);
    }

    private static Map<String, Object> mergeContext(Map<String, Object> context, String userId) {
        Map<String, Object> merged = new HashMap<>();
        if (context != null) {
            merged.putAll(context);
        }
        merged.put("userId", userId);
        return merged;
    }

}
