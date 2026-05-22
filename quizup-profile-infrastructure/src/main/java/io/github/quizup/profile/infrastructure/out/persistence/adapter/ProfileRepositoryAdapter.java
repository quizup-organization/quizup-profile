package io.github.quizup.profile.infrastructure.out.persistence.adapter;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.common.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.common.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.port.out.ProfileRepositoryPort;
import io.github.quizup.profile.infrastructure.out.persistence.entity.ProfileEntity;
import io.github.quizup.profile.infrastructure.out.persistence.mapper.ProfileEntityMapper;
import io.github.quizup.profile.infrastructure.out.persistence.repository.ProfileJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class ProfileRepositoryAdapter implements ProfileRepositoryPort {

    private final ProfileJpaRepository profileJpaRepository;
    private final JpaSearchAdapter<ProfileEntity> searchAdapter;

    public ProfileRepositoryAdapter(ProfileJpaRepository profileJpaRepository) {
        this.profileJpaRepository = profileJpaRepository;
        this.searchAdapter = new JpaSearchAdapter<>(
                profileJpaRepository,
                new AnnotationSearchableEntity(ProfileEntity.class)
        );
    }

    @Override
    @Transactional
    public void save(Profile profile) {
        profileJpaRepository.save(ProfileEntityMapper.toEntity(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Profile> findById(String profileId) {
        return profileJpaRepository.findById(profileId).map(ProfileEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String profileId) {
        return profileJpaRepository.existsById(profileId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Profile> findAll(SearchCriteria searchCriteria) {
        return searchAdapter.findAll(searchCriteria).map(ProfileEntityMapper::toDomain);
    }
}

