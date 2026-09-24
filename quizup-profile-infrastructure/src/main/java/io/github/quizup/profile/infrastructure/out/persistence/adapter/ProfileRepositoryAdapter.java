package io.github.quizup.profile.infrastructure.out.persistence.adapter;

import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.microservice.core.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.microservice.core.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.port.out.ProfileRepositoryPort;
import io.github.quizup.profile.infrastructure.out.persistence.entity.ProfileEntity;
import io.github.quizup.profile.infrastructure.out.persistence.mapper.ProfileEntityMapper;
import io.github.quizup.profile.infrastructure.out.persistence.repository.ProfileJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ProfileRepositoryAdapter implements ProfileRepositoryPort {

    private final ProfileJpaRepository profileJpaRepository;
    private final JpaSearchAdapter<ProfileEntity> profileJpaSearchAdapter;

    public ProfileRepositoryAdapter(ProfileJpaRepository profileJpaRepository) {
        this.profileJpaRepository = profileJpaRepository;
        this.profileJpaSearchAdapter = new JpaSearchAdapter<>(profileJpaRepository, new AnnotationSearchableEntity(ProfileEntity.class));
    }

    @Override
    public void save(Profile profile) {
        profileJpaRepository.save(ProfileEntityMapper.toEntity(profile));
    }

    @Override
    public Optional<Profile> findById(String userId) {
        return profileJpaRepository.findById(userId).map(ProfileEntityMapper::toDomain);
    }

    @Override
    public List<Profile> findByIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return profileJpaRepository.findAllById(userIds).stream()
                .map(ProfileEntityMapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<Profile> findAll(SearchCriteria searchCriteria) {
        return profileJpaSearchAdapter.findAll(searchCriteria, ProfileEntityMapper::toDomain);
    }

    @Override
    public boolean existsById(String userId) {
        return profileJpaRepository.existsById(userId);
    }
}
