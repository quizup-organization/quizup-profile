package io.github.quizup.profile.infrastructure.out.persistence.adapter;

import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.port.out.ProfileRepositoryPort;
import io.github.quizup.profile.infrastructure.out.persistence.mapper.ProfileEntityMapper;
import io.github.quizup.profile.infrastructure.out.persistence.repository.ProfileJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProfileRepositoryAdapter implements ProfileRepositoryPort {

    private final ProfileJpaRepository profileJpaRepository;

    public ProfileRepositoryAdapter(ProfileJpaRepository profileJpaRepository) {
        this.profileJpaRepository = profileJpaRepository;
    }

    @Override
    public void save(Profile profile) {
        profileJpaRepository.save(ProfileEntityMapper.toEntity(profile));
    }

    @Override
    public Optional<Profile> findById(String profileId) {
        return profileJpaRepository.findById(profileId).map(ProfileEntityMapper::toDomain);
    }

    @Override
    public boolean existsById(String profileId) {
        return profileJpaRepository.existsById(profileId);
    }
}

