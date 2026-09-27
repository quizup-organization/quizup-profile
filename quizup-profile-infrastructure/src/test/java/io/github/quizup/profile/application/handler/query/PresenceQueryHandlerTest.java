package io.github.quizup.profile.application.handler.query;

import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.domain.model.PresenceStatus;
import io.github.quizup.profile.domain.port.out.PresenceRepositoryPort;
import io.github.quizup.profile.domain.query.PresenceQuery;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PresenceQueryHandlerTest {

    private final PresenceRepositoryPort presenceRepository = mock(PresenceRepositoryPort.class);
    private final PresenceQueryHandler handler = new PresenceQueryHandler(presenceRepository);

    @Test
    void presences_by_ids_delegates_and_returns_found_rows_only() {
        PlayerPresence online = PlayerPresence.builder()
                .userId("user-1")
                .status(PresenceStatus.ONLINE)
                .lastSeenAt(Instant.parse("2026-09-27T10:00:00Z"))
                .build();
        when(presenceRepository.findByIds(List.of("user-1", "user-2"))).thenReturn(List.of(online));

        List<PlayerPresence> result = handler.handle(
                new PresenceQuery.GetPresencesByIdsQuery(List.of("user-1", "user-2")));

        assertThat(result).containsExactly(online);
        verify(presenceRepository).findByIds(List.of("user-1", "user-2"));
    }
}
