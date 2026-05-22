package io.github.quizup.profile.infrastructure.out.persistence.repository;

import io.github.quizup.profile.infrastructure.out.persistence.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileJpaRepository extends JpaRepository<ProfileEntity, String> {
}

