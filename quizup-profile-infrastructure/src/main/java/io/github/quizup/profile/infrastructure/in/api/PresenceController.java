package io.github.quizup.profile.infrastructure.in.api;

import io.github.quizup.microservice.core.domain.model.search.SearchCriteria;
import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.PageResponse;
import io.github.quizup.microservice.core.infrastructure.mapper.SearchRequestMapper;
import io.github.quizup.profile.domain.port.in.PresenceUseCase;
import io.github.quizup.profile.domain.port.in.SearchPresenceUseCase;
import io.github.quizup.profile.infrastructure.in.api.mapper.PresenceResponseMapper;
import io.github.quizup.profile.infrastructure.in.api.response.PresenceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * API REST de la présence joueur : lecture unitaire et recherche paginée standard. La présence
 * est un read-model éphémère alimenté par le cycle de vie des sessions temps réel (pas de
 * battement de cœur côté client).
 */
@RestController
@RequestMapping(PresenceController.ENDPOINT)
public class PresenceController {

    public static final String ENDPOINT = "/api/presence";

    private final PresenceUseCase presenceUseCase;
    private final SearchPresenceUseCase searchPresenceUseCase;

    public PresenceController(PresenceUseCase presenceUseCase,
                              SearchPresenceUseCase searchPresenceUseCase) {
        this.presenceUseCase = presenceUseCase;
        this.searchPresenceUseCase = searchPresenceUseCase;
    }

    /** Présence d'un joueur (un inconnu est retourné {@code OFFLINE}). */
    @GetMapping("/{userId}")
    public ResponseEntity<PresenceResponse> get(@PathVariable String userId) {
        return ResponseEntity.ok(PresenceResponseMapper.toResponse(presenceUseCase.get(userId)));
    }

    /** Recherche paginée de présences (filtres/sorts/pagination standards). */
    @PostMapping("/search")
    public CompletableFuture<ResponseEntity<PageResponse<PresenceResponse>>> search(
            @RequestBody SearchRequest searchRequest) {
        SearchCriteria searchCriteria = SearchRequestMapper.toSearchCriteria(searchRequest);
        return searchPresenceUseCase
                .search(
                        searchCriteria.filters(),
                        searchCriteria.sorts(),
                        searchCriteria.page()
                )
                .thenApply(PresenceResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }
}
