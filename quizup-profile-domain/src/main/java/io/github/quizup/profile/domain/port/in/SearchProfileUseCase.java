package io.github.quizup.profile.domain.port.in;

import io.github.quizup.common.domain.model.search.FilterCriteria;
import io.github.quizup.common.domain.model.search.PageCriteria;
import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SortCriteria;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.query.ProfileQuery;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SearchProfileUseCase {

    CompletableFuture<PageResult<Profile>> search(ProfileQuery.SearchProfileQuery query);

    default CompletableFuture<PageResult<Profile>> search(List<FilterCriteria> filters,
                                                          List<SortCriteria> sorts,
                                                          PageCriteria page) {
        return search(new ProfileQuery.SearchProfileQuery(filters, sorts, page));
    }
}

