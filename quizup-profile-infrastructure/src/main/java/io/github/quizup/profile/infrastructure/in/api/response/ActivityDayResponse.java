package io.github.quizup.profile.infrastructure.in.api.response;

import java.time.LocalDate;

/**
 * DTO d'un point d'activité journalière (date + nombre de parties).
 */
public record ActivityDayResponse(
        LocalDate date,
        int games
) {
}
