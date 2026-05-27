package io.github.quizup.profile.infrastructure.out.persistence.adapter;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.common.domain.model.search.SortCriteria;
import io.github.quizup.common.domain.model.search.SortDirection;
import io.github.quizup.common.domain.model.search.DefaultPageResult;
import io.github.quizup.common.domain.model.search.DefaultSortCriteria;
import io.github.quizup.common.infrastructure.adapter.AnnotationSearchableEntity;
import io.github.quizup.common.infrastructure.adapter.JpaSearchAdapter;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.model.ProfileTopic;
import io.github.quizup.profile.domain.port.out.ProfileRepositoryPort;
import io.github.quizup.profile.infrastructure.out.persistence.entity.ProfileEntity;
import io.github.quizup.profile.infrastructure.out.persistence.mapper.ProfileEntityMapper;
import io.github.quizup.profile.infrastructure.out.persistence.repository.ProfileJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;

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

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProfileTopic> findTopicsByProfileId(String profileId, SearchCriteria searchCriteria) {
        Optional<Profile> maybeProfile = findById(profileId);
        if (maybeProfile.isEmpty()) {
            return PageResult.unpaged();
        }

        int pageNumber = Optional.ofNullable(searchCriteria)
                .map(SearchCriteria::page)
                .map(p -> p.number() == null ? 0 : p.number())
                .orElse(0);
        int pageSize = Optional.ofNullable(searchCriteria)
                .map(SearchCriteria::page)
                .map(p -> p.size() == null ? 20 : p.size())
                .orElse(20);

        List<ProfileTopic> topics = maybeProfile.get().topics().values().stream().toList();
        if (topics.isEmpty()) {
            return new DefaultPageResult<>(
                    Collections.emptyList(),
                    pageNumber,
                    pageSize,
                    0,
                    0,
                    Collections.emptyList(),
                    true,
                    true,
                    true
            );
        }

        List<SortCriteria> sorts = Optional.ofNullable(searchCriteria)
                .map(SearchCriteria::sorts)
                .orElse(Collections.emptyList());
        if (sorts.isEmpty()) {
            sorts = List.of(
                    new DefaultSortCriteria("totalExperience", SortDirection.DESC),
                    new DefaultSortCriteria("wins", SortDirection.DESC)
            );
        }

        Comparator<ProfileTopic> comparator = toComparator(sorts.getFirst());
        for (int i = 1; i < sorts.size(); i++) {
            comparator = comparator.thenComparing(toComparator(sorts.get(i)));
        }

        List<ProfileTopic> sorted = topics.stream().sorted(comparator).toList();


        if (pageSize <= 0) {
            pageSize = sorted.size();
        }

        int fromIndex = Math.max(0, pageNumber * pageSize);
        int toIndex = Math.min(sorted.size(), fromIndex + pageSize);
        List<ProfileTopic> content = fromIndex >= sorted.size()
                ? Collections.emptyList()
                : sorted.subList(fromIndex, toIndex);

        int totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) sorted.size() / pageSize);
        return new DefaultPageResult<>(
                content,
                pageNumber,
                pageSize,
                sorted.size(),
                totalPages,
                sorts,
                pageNumber == 0,
                pageNumber >= Math.max(0, totalPages - 1),
                content.isEmpty()
        );
    }

    private Comparator<ProfileTopic> toComparator(SortCriteria sort) {
        Comparator<ProfileTopic> comparator = switch (sort.property()) {
            case "topicId" -> Comparator.comparing(ProfileTopic::topicId, Comparator.nullsLast(String::compareTo));
            case "wins" -> Comparator.comparingInt(ProfileTopic::wins);
            case "losses" -> Comparator.comparingInt(ProfileTopic::losses);
            case "draws" -> Comparator.comparingInt(ProfileTopic::draws);
            case "totalExperience" -> Comparator.comparingInt(ProfileTopic::totalExperience);
            default -> Comparator.comparingInt(ProfileTopic::totalExperience);
        };

        return sort.direction() == SortDirection.ASC ? comparator : comparator.reversed();
    }
}

