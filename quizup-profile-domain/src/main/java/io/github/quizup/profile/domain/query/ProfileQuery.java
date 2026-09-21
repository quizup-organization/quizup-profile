package io.github.quizup.profile.domain.query;

import io.github.quizup.microservice.core.domain.model.search.FilterCriteria;
import io.github.quizup.microservice.core.domain.model.search.PageCriteria;
import io.github.quizup.microservice.core.domain.model.search.SortCriteria;
import io.github.quizup.microservice.core.domain.query.SearchQuery;

import java.util.List;

public interface ProfileQuery {

    record GetProfileQuery(String userId) implements ProfileQuery {
    }

    record ProfileExistsByIdQuery(String userId) implements ProfileQuery {
    }

    record ProfileSearchQuery(
            List<FilterCriteria> filters,
            List<SortCriteria> sorts,
            PageCriteria page
    ) implements ProfileQuery, SearchQuery {
    }
}
