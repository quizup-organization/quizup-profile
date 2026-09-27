package io.github.quizup.profile.application.handler.query;

import io.github.quizup.profile.domain.model.GameXp;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.port.out.ProgressionAwardedGameRepositoryPort;
import io.github.quizup.profile.domain.port.out.ProgressionRepositoryPort;
import io.github.quizup.profile.domain.query.ProgressionQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProgressionQueryHandlerTest {

    private final ProgressionRepositoryPort progressionRepository = mock(ProgressionRepositoryPort.class);
    private final ProgressionAwardedGameRepositoryPort awardedGameRepository =
            mock(ProgressionAwardedGameRepositoryPort.class);
    private final ProgressionQueryHandler handler =
            new ProgressionQueryHandler(progressionRepository, awardedGameRepository);

    @Test
    void games_xp_delegates_to_awarded_journal() {
        when(awardedGameRepository.findGameXp("me", List.of("game-1", "game-2")))
                .thenReturn(List.of(new GameXp("game-1", 120)));

        List<GameXp> result = handler.handle(
                new ProgressionQuery.GetGamesXpQuery("me", List.of("game-1", "game-2")));

        assertThat(result).containsExactly(new GameXp("game-1", 120));
        verify(awardedGameRepository).findGameXp("me", List.of("game-1", "game-2"));
    }

    @Test
    void progressions_by_ids_returns_empty_progression_for_unknown_users() {
        when(progressionRepository.findByIds(List.of("known", "unknown")))
                .thenReturn(List.of(PlayerProgress.empty("known")));

        List<PlayerProgress> result = handler.handle(
                new ProgressionQuery.GetProgressionsByIdsQuery(List.of("known", "unknown")));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).userId()).isEqualTo("known");
        assertThat(result.get(1).userId()).isEqualTo("unknown");
        assertThat(result.get(1).level()).isEqualTo(1);
    }
}
