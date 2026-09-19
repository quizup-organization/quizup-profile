package io.github.quizup.profile.infrastructure.in.api;

import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.PageResponse;
import io.github.quizup.microservice.core.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.microservice.security.SecurityHelper;
import io.github.quizup.profile.domain.port.in.GetProfileUseCase;
import io.github.quizup.profile.domain.port.in.SearchProfileUseCase;
import io.github.quizup.profile.domain.port.in.UpdateProfileUseCase;
import io.github.quizup.profile.infrastructure.in.api.mapper.ProfileResponseMapper;
import io.github.quizup.profile.infrastructure.in.api.request.UpdateProfileRequest;
import io.github.quizup.profile.infrastructure.in.api.response.ProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * ProfileController - API REST pour les profils
 */
@RestController
@RequestMapping("/api/profiles")
@CrossOrigin(origins = "*")
public class ProfileController {

    private final GetProfileUseCase getProfileUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final SearchProfileUseCase searchProfileUseCase;

    public ProfileController(GetProfileUseCase getProfileUseCase,
                             UpdateProfileUseCase updateProfileUseCase,
                             SearchProfileUseCase searchProfileUseCase) {
        this.getProfileUseCase = getProfileUseCase;
        this.updateProfileUseCase = updateProfileUseCase;
        this.searchProfileUseCase = searchProfileUseCase;
    }

    /**
     * Récupérer un profil par son userId
     */
    @GetMapping("/{userId}")
    public CompletableFuture<ResponseEntity<ProfileResponse>> getProfileById(@PathVariable String userId) {
        return getProfileUseCase.getById(userId)
                .thenApply(ProfileResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    /**
     * Mettre à jour son propre profil (403 si le JWT ne porte pas ce userId)
     */
    @PutMapping("/{userId}")
    public CompletableFuture<ResponseEntity<Void>> updateProfile(
            @PathVariable String userId,
            @RequestBody UpdateProfileRequest request) {
        return updateProfileUseCase.update(
                        userId,
                        SecurityHelper.getUserId(),
                        request.displayName(),
                        request.bio(),
                        request.country())
                .thenApply(updatedId -> ResponseEntity.ok().build());
    }

    /**
     * Search profiles with pagination and sorting (displayName / email)
     */
    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<PageResponse<ProfileResponse>>> search(@RequestBody SearchRequest searchRequest) {
        SearchCriteria searchCriteria = SearchRequestMapper.toSearchCriteria(searchRequest);
        return searchProfileUseCase
                .search(
                        searchCriteria.filters(),
                        searchCriteria.sorts(),
                        searchCriteria.page()
                )
                .thenApply(ProfileResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }
}
