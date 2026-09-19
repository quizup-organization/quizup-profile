package io.github.quizup.profile.infrastructure.in.api;

import io.github.quizup.profile.domain.port.in.GetProgressionUseCase;
import io.github.quizup.profile.infrastructure.in.api.mapper.ProgressionResponseMapper;
import io.github.quizup.profile.infrastructure.in.api.response.ProgressionResponse;
import io.github.quizup.profile.infrastructure.in.api.response.TopicProgressResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * ProgressionController — API REST de progression (XP/niveau/titres/badges).
 */
@RestController
@RequestMapping("/api/profiles")
@CrossOrigin(origins = "*")
public class ProgressionController {

    private final GetProgressionUseCase getProgressionUseCase;

    public ProgressionController(GetProgressionUseCase getProgressionUseCase) {
        this.getProgressionUseCase = getProgressionUseCase;
    }

    /**
     * Progression globale d'un joueur (niveau 1 / 0 XP si aucun duel joué).
     */
    @GetMapping("/{userId}/progress")
    public CompletableFuture<ResponseEntity<ProgressionResponse>> getProgress(@PathVariable String userId) {
        return getProgressionUseCase.getById(userId)
                .thenApply(ProgressionResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }

    /**
     * Progression d'un joueur dans un thème donné.
     */
    @GetMapping("/{userId}/progress/{topicId}")
    public CompletableFuture<ResponseEntity<TopicProgressResponse>> getTopicProgress(
            @PathVariable String userId,
            @PathVariable String topicId) {
        return getProgressionUseCase.getTopic(userId, topicId)
                .thenApply(ProgressionResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }
}
