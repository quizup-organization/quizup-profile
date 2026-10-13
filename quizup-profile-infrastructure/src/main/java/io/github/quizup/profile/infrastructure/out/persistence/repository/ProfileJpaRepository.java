package io.github.quizup.profile.infrastructure.out.persistence.repository;

import io.github.quizup.profile.infrastructure.out.persistence.entity.ProfileEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repository pour ProfileEntity
 */
@Repository
public interface ProfileJpaRepository extends JpaRepository<ProfileEntity, String>, JpaSpecificationExecutor<ProfileEntity> {

    List<ProfileEntity> findByPseudonymContainingIgnoreCaseOrderByPseudonymAsc(String nameQuery, Limit limit);

    @Query("""
            select p.bannerUrl, count(p)
            from ProfileEntity p
            where p.bannerUrl in :urls
            group by p.bannerUrl
            """)
    List<Object[]> countByBannerUrlIn(@Param("urls") Collection<String> urls);
}
