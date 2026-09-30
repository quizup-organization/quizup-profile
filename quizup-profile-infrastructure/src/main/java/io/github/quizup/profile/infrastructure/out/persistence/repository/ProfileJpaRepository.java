package io.github.quizup.profile.infrastructure.out.persistence.repository;

import io.github.quizup.profile.infrastructure.out.persistence.entity.ProfileEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour ProfileEntity
 */
@Repository
public interface ProfileJpaRepository extends JpaRepository<ProfileEntity, String>, JpaSpecificationExecutor<ProfileEntity> {

    List<ProfileEntity> findByPseudonymContainingIgnoreCaseOrderByPseudonymAsc(String nameQuery, Limit limit);
}
