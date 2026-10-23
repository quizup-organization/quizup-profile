package io.github.quizup.profile.application.handler.query;

import io.github.quizup.profile.domain.model.GameXp;
import io.github.quizup.profile.domain.model.MasteryGrade;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.TopicProgress;
import io.github.quizup.profile.domain.port.out.ProgressionAwardedGameRepositoryPort;
import io.github.quizup.profile.domain.port.out.ProgressionRepositoryPort;
import io.github.quizup.profile.domain.query.ProgressionQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                .thenReturn(List.of(new GameXp("game-1", 120, 18, true)));

        List<GameXp> result = handler.handle(
                new ProgressionQuery.GetGamesXpQuery("me", List.of("game-1", "game-2")));

        assertThat(result).containsExactly(new GameXp("game-1", 120, 18, true));
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
        assertThat(result.get(1).xpByTopic()).isEmpty();
    }

    @Test
    void topic_progressions_are_sorted_by_games_then_xp() {
        when(progressionRepository.findById("me")).thenReturn(Optional.of(
                PlayerProgress.empty("me").toBuilder()
                        .xpByTopic(Map.of("dota", 500, "space", 200, "frozen", 300))
                        .gamesByTopic(Map.of("dota", 3, "space", 7, "frozen", 3))
                        .build()));

        List<TopicProgress> result = handler.handle(
                new ProgressionQuery.GetTopicProgressionsQuery("me"));

        assertThat(result).hasSize(3);
        assertThat(result).extracting(TopicProgress::topicId)
                .containsExactly("space", "dota", "frozen");
        assertThat(result.get(0).games()).isEqualTo(7);
        assertThat(result.get(0).xp()).isEqualTo(200);
        assertThat(result.get(0).grade()).isEqualTo(MasteryGrade.NOVICE);
        assertThat(result.get(1).games()).isEqualTo(3);
    }

    @Test
    void topic_progressions_keep_bot_only_topics_with_zero_games() {
        when(progressionRepository.findById("me")).thenReturn(Optional.of(
                PlayerProgress.empty("me").toBuilder()
                        .xpByTopic(Map.of("bot-topic", 120))
                        .gamesByTopic(Map.of())
                        .build()));

        List<TopicProgress> result = handler.handle(
                new ProgressionQuery.GetTopicProgressionsQuery("me"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).topicId()).isEqualTo("bot-topic");
        assertThat(result.get(0).xp()).isEqualTo(120);
        assertThat(result.get(0).games()).isZero();
    }

    @Test
    void topic_progressions_return_empty_for_unknown_user() {
        when(progressionRepository.findById("unknown")).thenReturn(Optional.empty());

        List<TopicProgress> result = handler.handle(
                new ProgressionQuery.GetTopicProgressionsQuery("unknown"));

        assertThat(result).isEmpty();
    }
}
