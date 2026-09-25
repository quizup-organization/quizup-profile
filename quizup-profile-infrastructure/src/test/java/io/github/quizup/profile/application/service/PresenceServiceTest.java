package io.github.quizup.profile.application.service;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.profile.domain.event.PresenceEvent;
import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.domain.model.PresenceDeadline;
import io.github.quizup.profile.domain.model.PresenceRules;
import io.github.quizup.profile.domain.model.PresenceStatus;
import io.github.quizup.profile.domain.port.out.PresenceRepositoryPort;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.messaging.NoScopeDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PresenceServiceTest {

    private InMemoryPresenceRepository repository;
    private EventGateway eventGateway;
    private DeadlineManager deadlineManager;
    private PresenceService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPresenceRepository();
        eventGateway = mock(EventGateway.class);
        deadlineManager = mock(DeadlineManager.class);
        service = new PresenceService(repository, eventGateway, deadlineManager);
    }

    @Test
    void firstSessionMarksPlayerOnlineAndPublishesOnlineEvent() {
        PlayerPresence presence = service.sessionConnected("s1", "u1");

        assertThat(presence.status()).isEqualTo(PresenceStatus.ONLINE);
        assertThat(presence.lastSeenAt()).isNotNull();
        verify(eventGateway, times(1)).publish(any(PresenceEvent.PlayerWentOnlineEvent.class));
    }

    @Test
    void additionalSessionDoesNotPublishOnlineEventAgain() {
        service.sessionConnected("s1", "u1");
        service.sessionConnected("s2", "u1");

        verify(eventGateway, times(1)).publish(any(PresenceEvent.PlayerWentOnlineEvent.class));
    }

    @Test
    void closingLastSessionSchedulesOfflineGraceAndStaysOnline() {
        service.sessionConnected("s1", "u1");

        PlayerPresence presence = service.sessionDisconnected("s1", "u1");

        assertThat(presence.status()).isEqualTo(PresenceStatus.ONLINE);
        assertThat(presence.lastSeenAt()).isNotNull();
        verify(deadlineManager).schedule(
                eq(PresenceRules.DISCONNECT_GRACE),
                eq(PresenceDeadline.OFFLINE),
                any(PresenceDeadline.OfflineCheck.class),
                eq(NoScopeDescriptor.INSTANCE));
    }

    @Test
    void closingSessionWithAnotherOpenSessionDoesNotSchedule() {
        service.sessionConnected("s1", "u1");
        service.sessionConnected("s2", "u1");

        service.sessionDisconnected("s1", "u1");

        verify(deadlineManager, never()).schedule(any(Duration.class), anyString(), any(), any());
    }

    @Test
    void confirmOfflineTurnsPlayerOfflineAndPublishesEvent() {
        service.sessionConnected("s1", "u1");
        service.sessionDisconnected("s1", "u1");

        service.confirmOffline("u1");

        assertThat(service.get("u1").status()).isEqualTo(PresenceStatus.OFFLINE);
        verify(eventGateway).publish(any(PresenceEvent.PlayerWentOfflineEvent.class));
    }

    @Test
    void confirmOfflineIsNoOpWhenReconnected() {
        service.sessionConnected("s1", "u1");
        service.sessionDisconnected("s1", "u1");
        service.sessionConnected("s2", "u1");

        service.confirmOffline("u1");

        assertThat(service.get("u1").status()).isEqualTo(PresenceStatus.ONLINE);
        verify(eventGateway, never()).publish(any(PresenceEvent.PlayerWentOfflineEvent.class));
    }

    @Test
    void unknownPlayerIsOffline() {
        assertThat(service.get("unknown").status()).isEqualTo(PresenceStatus.OFFLINE);
    }

    private static final class InMemoryPresenceRepository implements PresenceRepositoryPort {

        private final Map<String, PlayerPresence> presences = new HashMap<>();
        private final Map<String, String> sessionOwners = new HashMap<>();

        @Override
        public PlayerPresence save(PlayerPresence presence) {
            presences.put(presence.userId(), presence);
            return presence;
        }

        @Override
        public Optional<PlayerPresence> findById(String userId) {
            return Optional.ofNullable(presences.get(userId));
        }

        @Override
        public SearchResponse<PlayerPresence> findAll(SearchRequest request) {
            return new SearchResponse<>(List.of(), 0, 0, 0, 0, List.of(), true, true, true);
        }

        @Override
        public void addSession(String sessionId, String userId) {
            sessionOwners.put(sessionId, userId);
        }

        @Override
        public void removeSession(String sessionId) {
            sessionOwners.remove(sessionId);
        }

        @Override
        public long countSessions(String userId) {
            return sessionOwners.values().stream().filter(userId::equals).count();
        }
    }
}
