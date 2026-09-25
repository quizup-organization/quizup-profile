package io.github.quizup.profile.domain.query;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;

public interface PresenceQuery {

    record PresenceSearchQuery(SearchRequest request) implements PresenceQuery {
    }
}
