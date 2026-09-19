package io.github.quizup.profile.infrastructure.in.api;

import io.github.quizup.profile.domain.port.in.GetActivityUseCase;
import io.github.quizup.profile.infrastructure.in.api.mapper.ActivityResponseMapper;
import io.github.quizup.profile.infrastructure.in.api.response.ActivityResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * API REST de l'activité journalière (streak + graphe de contribution). Endpoint séparé de la
 * progression pour rester composable côté client (fiche joueur, batch).
 */
@RestController
@RequestMapping("/api/profiles")
public class ActivityController {

    private static final int DEFAULT_WINDOW_DAYS = 364;

    private final GetActivityUseCase getActivityUseCase;

    public ActivityController(GetActivityUseCase getActivityUseCase) {
        this.getActivityUseCase = getActivityUseCase;
    }

    /**
     * Activité d'un joueur sur {@code [from, to]} (défaut : 365 derniers jours). {@code from}/{@code to}
     * sont optionnels au format ISO {@code yyyy-MM-dd}.
     */
    @GetMapping("/{userId}/activity")
    public ResponseEntity<ActivityResponse> getActivity(
            @PathVariable String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_WINDOW_DAYS);

        return ResponseEntity.ok(
                ActivityResponseMapper.toResponse(getActivityUseCase.get(userId, start, end)));
    }
}
