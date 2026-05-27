package io.github.quizup.profile.domain.port.in;

import io.github.quizup.common.domain.model.search.FilterCriteria;
import io.github.quizup.common.domain.model.search.PageCriteria;
import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SortCriteria;
import io.github.quizup.profile.domain.model.ProfileTopic;
import io.github.quizup.profile.domain.query.ProfileQuery;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SearchProfileTopicsUseCase {

    CompletableFuture<PageResult<ProfileTopic>> searchTopics(ProfileQuery.SearchProfileTopicsQuery query);

    default CompletableFuture<PageResult<ProfileTopic>> searchTopics(String profileId,
                                                                      List<FilterCriteria> filters,
                                                                      List<SortCriteria> sorts,
                                                                      PageCriteria page) {
        return searchTopics(new ProfileQuery.SearchProfileTopicsQuery(profileId, filters, sorts, page));
    }
}

