package io.github.quizup.profile.infrastructure.in.api;

import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.common.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.common.infrastructure.in.api.response.PageResponse;
import io.github.quizup.common.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.profile.domain.port.in.GetProfileUseCase;
import io.github.quizup.profile.domain.port.in.SearchProfileUseCase;
import io.github.quizup.profile.infrastructure.in.api.mapper.ProfileResponseMapper;
import io.github.quizup.profile.infrastructure.in.api.response.ProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(ProfileController.ENDPOINT)
public class ProfileController {

    public static final String ENDPOINT = "/api/profiles";

    private final GetProfileUseCase getProfileUseCase;
    private final SearchProfileUseCase searchProfileUseCase;

    public ProfileController(GetProfileUseCase getProfileUseCase,
                             SearchProfileUseCase searchProfileUseCase) {
        this.getProfileUseCase = getProfileUseCase;
        this.searchProfileUseCase = searchProfileUseCase;
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
}

