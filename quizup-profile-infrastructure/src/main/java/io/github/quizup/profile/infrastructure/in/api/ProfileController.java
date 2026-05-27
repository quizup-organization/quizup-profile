package io.github.quizup.profile.infrastructure.in.api;

import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.common.domain.model.search.DefaultPageCriteria;
import io.github.quizup.common.domain.model.search.DefaultSortCriteria;
import io.github.quizup.common.domain.model.search.PageCriteria;
import io.github.quizup.common.domain.model.search.SortCriteria;
import io.github.quizup.common.domain.model.search.SortDirection;
import io.github.quizup.common.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.common.infrastructure.in.api.response.PageResponse;
import io.github.quizup.common.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.profile.domain.port.in.GetProfileUseCase;
import io.github.quizup.profile.domain.port.in.SearchProfileUseCase;
import io.github.quizup.profile.domain.port.in.SearchProfileTopicsUseCase;
import io.github.quizup.profile.infrastructure.in.api.mapper.ProfileResponseMapper;
import io.github.quizup.profile.infrastructure.in.api.response.ProfileResponse;
import io.github.quizup.profile.infrastructure.in.api.response.ProfileTopicResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(ProfileController.ENDPOINT)
public class ProfileController {

    public static final String ENDPOINT = "/api/profiles";

    private final GetProfileUseCase getProfileUseCase;
    private final SearchProfileUseCase searchProfileUseCase;
    private final SearchProfileTopicsUseCase searchProfileTopicsUseCase;

    public ProfileController(GetProfileUseCase getProfileUseCase,
                             SearchProfileUseCase searchProfileUseCase,
                             SearchProfileTopicsUseCase searchProfileTopicsUseCase) {
        this.getProfileUseCase = getProfileUseCase;
        this.searchProfileUseCase = searchProfileUseCase;
        this.searchProfileTopicsUseCase = searchProfileTopicsUseCase;
    }

    @GetMapping("/{profileId}")
    public CompletableFuture<ResponseEntity<ProfileResponse>> getById(@PathVariable String profileId) {
        return getProfileUseCase.getById(profileId)
                .thenApply(ProfileResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<PageResponse<ProfileResponse>>> search(@RequestBody SearchRequest searchRequest) {
        SearchCriteria criteria = SearchRequestMapper.toSearchCriteria(searchRequest);
        return searchProfileUseCase.search(criteria.filters(), criteria.sorts(), criteria.page())
                .thenApply(ProfileResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/{profileId}/topics")
    public CompletableFuture<ResponseEntity<PageResponse<ProfileTopicResponse>>> searchTopics(
            @PathVariable String profileId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) List<String> sort,
            @RequestParam(defaultValue = "DESC") SortDirection direction
    ) {
        PageCriteria pageCriteria = new DefaultPageCriteria(size, page);
        List<SortCriteria> sorts = buildSorts(sort, direction);

        return searchProfileTopicsUseCase.searchTopics(profileId, Collections.emptyList(), sorts, pageCriteria)
                .thenApply(ProfileResponseMapper::toTopicResponse)
                .thenApply(ResponseEntity::ok);
    }

    private List<SortCriteria> buildSorts(List<String> properties, SortDirection direction) {
        if (properties == null || properties.isEmpty()) {
            return List.of(
                    new DefaultSortCriteria("totalExperience", SortDirection.DESC),
                    new DefaultSortCriteria("wins", SortDirection.DESC)
            );
        }

        return properties.stream()
                .map(property -> new DefaultSortCriteria(property, direction))
                .map(SortCriteria.class::cast)
                .toList();
    }
}

