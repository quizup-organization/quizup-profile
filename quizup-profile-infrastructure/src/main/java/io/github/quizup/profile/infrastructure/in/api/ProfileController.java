package io.github.quizup.profile.infrastructure.in.api;

import io.github.quizup.profile.domain.port.in.GetProfileUseCase;
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

    public ProfileController(GetProfileUseCase getProfileUseCase) {
        this.getProfileUseCase = getProfileUseCase;
    }

    @GetMapping("/{profileId}")
    public CompletableFuture<ResponseEntity<ProfileResponse>> getById(@PathVariable String profileId) {
        return getProfileUseCase.getById(profileId)
                .thenApply(ProfileResponseMapper::toResponse)
                .thenApply(ResponseEntity::ok);
    }
}

