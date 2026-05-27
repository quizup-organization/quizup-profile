package io.github.quizup.profile.domain.query;

import io.github.quizup.common.domain.model.search.FilterCriteria;
import io.github.quizup.common.domain.model.search.PageCriteria;
import io.github.quizup.common.domain.model.search.SortCriteria;
import io.github.quizup.common.domain.query.SearchQuery;

import java.util.List;

public interface ProfileQuery {

	record GetProfileByIdQuery(String profileId) implements ProfileQuery {
	}

	record ProfileExistsByIdQuery(String profileId) implements ProfileQuery {
	}

	record SearchProfileQuery(
			List<FilterCriteria> filters,
			List<SortCriteria> sorts,
			PageCriteria page
	) implements ProfileQuery, SearchQuery {
	}

	record SearchProfileTopicsQuery(
			String profileId,
			List<FilterCriteria> filters,
			List<SortCriteria> sorts,
			PageCriteria page
	) implements ProfileQuery, SearchQuery {
	}
}

