package io.github.quizup.profile.domain.exception;

import io.github.quizup.common.domain.exception.BaseProblem;
import io.github.quizup.common.domain.exception.ProblemCategory;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class ProfileProblem extends BaseProblem {

    private final String profileId;

    protected ProfileProblem(
            String profileId,
            String type,
            ProblemCategory category,
            String title,
            String detail,
            Map<String, Object> context) {
        super(type, category, title, detail, mergeContext(context, profileId));
        this.profileId = profileId;
    }

    protected ProfileProblem(
            String profileId,
            String type,
            String title,
            String detail,
            Map<String, Object> context) {
        this(profileId, type, ProblemCategory.BUSINESS_INVALID_COMMAND, title, detail, context);
    }

    private static Map<String, Object> mergeContext(Map<String, Object> context, String profileId) {
        Map<String, Object> merged = new HashMap<>();
        if (context != null) {
            merged.putAll(context);
        }
        merged.put("profileId", profileId);
        return merged;
    }
}

