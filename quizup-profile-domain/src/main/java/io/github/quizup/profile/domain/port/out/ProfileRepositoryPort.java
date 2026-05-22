package io.github.quizup.profile.domain.port.out;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.profile.domain.model.Profile;

import java.util.Optional;

public interface ProfileRepositoryPort {

    void save(Profile profile);

    Optional<Profile> findById(String profileId);

    boolean existsById(String profileId);

    PageResult<Profile> findAll(SearchCriteria searchCriteria);
}

