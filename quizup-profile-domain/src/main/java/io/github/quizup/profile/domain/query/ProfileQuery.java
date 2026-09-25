package io.github.quizup.profile.domain.query;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;

import java.util.List;

public interface ProfileQuery {

    record GetProfileQuery(String userId) implements ProfileQuery {
    }

    record ProfileExistsByIdQuery(String userId) implements ProfileQuery {
    }

    /**
     * Résolution en lot : évite le fan-out N+1 côté consommateur (ex. liste de suiveurs).
     */
    record GetProfilesByIdsQuery(List<String> userIds) implements ProfileQuery {
    }

    record ProfileSearchQuery(SearchRequest request) implements ProfileQuery {
    }
}
