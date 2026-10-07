package io.github.quizup.profile.application.service;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.profile.domain.event.PresenceEvent;
import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.domain.model.PresenceRules;
import io.github.quizup.profile.domain.model.PresenceStatus;
import io.github.quizup.profile.domain.port.out.PresenceLeasePort;
import io.github.quizup.profile.domain.port.out.PresenceRepositoryPort;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests de l'orchestration présence : leases (store chaud) + projection durable.
 */
class PresenceServiceTest {

    private InMemoryPresenceRepository repository;
    private InMemoryPresenceLease lease;
    private EventGateway eventGateway;
    private PresenceService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPresenceRepository();
        lease = new InMemoryPresenceLease();
        eventGateway = mock(EventGateway.class);
        service = new PresenceService(repository, lease, eventGateway);
    }

    @Test
    void firstSessionMarksPlayerOnlineAndPublishesOnlineEvent() {
        PlayerPresence presence = service.sessionConnected("s1", "u1", "bff:1");

        assertThat(presence.status()).isEqualTo(PresenceStatus.ONLINE);
        assertThat(presence.lastSeenAt()).isNotNull();
        verify(eventGateway, times(1)).publish(any(PresenceEvent.PlayerWentOnlineEvent.class));
    }

    @Test
    void additionalSessionDoesNotPublishOnlineEventAgain() {
        service.sessionConnected("s1", "u1", "bff:1");
        service.sessionConnected("s2", "u1", "bff:1");

        verify(eventGateway, times(1)).publish(any(PresenceEvent.PlayerWentOnlineEvent.class));
    }

    @Test
    void closingLastSessionKeepsPlayerOnlineDuringGrace() {
        service.sessionConnected("s1", "u1", "bff:1");

        PlayerPresence presence = service.sessionDisconnected("s1", "u1");

        assertThat(presence.status()).isEqualTo(PresenceStatus.ONLINE);
        assertThat(presence.lastSeenAt()).isNotNull();
        assertThat(lease.deadlineOf("u1")).isNotNull();
    }

    @Test
    void closingSessionWithAnotherOpenSessionArmsNoDeadline() {
        service.sessionConnected("s1", "u1", "bff:1");
        service.sessionConnected("s2", "u1", "bff:1");

        service.sessionDisconnected("s1", "u1");

        assertThat(lease.deadlineOf("u1")).isNull();
    }

    @Test
    void expiryTurnsPlayerOfflineAndPublishesEvent() {
        service.sessionConnected("s1", "u1", "bff:1");
        service.sessionDisconnected("s1", "u1");
        lease.forceDeadlineInThePast("u1");

        service.expireOfflineDeadlines();

        assertThat(service.get("u1").status()).isEqualTo(PresenceStatus.OFFLINE);
        verify(eventGateway).publish(any(PresenceEvent.PlayerWentOfflineEvent.class));
    }

    @Test
    void expiryIsNoOpWhenReconnected() {
        service.sessionConnected("s1", "u1", "bff:1");
        service.sessionDisconnected("s1", "u1");
        lease.forceDeadlineInThePast("u1");
        service.sessionConnected("s2", "u1", "bff:1");

        service.expireOfflineDeadlines();

        assertThat(service.get("u1").status()).isEqualTo(PresenceStatus.ONLINE);
        verify(eventGateway, never()).publish(any(PresenceEvent.PlayerWentOfflineEvent.class));
    }

    @Test
    void renewDelegatesToLeaseStore() {
        service.renewSessions(List.of("s1", "s2"));

        assertThat(lease.renewed).containsExactly("s1", "s2");
    }

    @Test
    void unknownPlayerIsOffline() {
        assertThat(service.get("unknown").status()).isEqualTo(PresenceStatus.OFFLINE);
    }

    private static final class InMemoryPresenceRepository implements PresenceRepositoryPort {

        private final Map<String, PlayerPresence> presences = new HashMap<>();

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
        public List<PlayerPresence> findByIds(List<String> userIds) {
            return userIds.stream()
                    .map(presences::get)
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    private static final class InMemoryPresenceLease implements PresenceLeasePort {

        private final Map<String, Set<String>> liveSessions = new HashMap<>();
        private final Map<String, Instant> offlineDeadlines = new HashMap<>();
        private final List<String> renewed = new ArrayList<>();

        @Override
        public boolean openSession(String userId, String sessionId) {
            boolean wasOnline = isOnline(userId);
            liveSessions.computeIfAbsent(userId, key -> new HashSet<>()).add(sessionId);
            offlineDeadlines.remove(userId);
            return !wasOnline;
        }

        @Override
        public boolean closeSession(String userId, String sessionId) {
            Set<String> sessions = liveSessions.computeIfAbsent(userId, key -> new HashSet<>());
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                offlineDeadlines.put(userId, Instant.now().plus(PresenceRules.DISCONNECT_GRACE));
                return false;
            }
            return true;
        }

        @Override
        public void renewSessions(List<String> sessionIds) {
            renewed.addAll(sessionIds);
        }

        @Override
        public List<String> claimDueOffline(Instant now) {
            List<String> claimed = new ArrayList<>();
            for (Map.Entry<String, Instant> entry : Map.copyOf(offlineDeadlines).entrySet()) {
                String userId = entry.getKey();
                if (!entry.getValue().isAfter(now) && !hasLiveSessions(userId)) {
                    offlineDeadlines.remove(userId);
                    liveSessions.remove(userId);
                    claimed.add(userId);
                }
            }
            return claimed;
        }

        void forceDeadlineInThePast(String userId) {
            offlineDeadlines.put(userId, Instant.now().minusSeconds(1));
        }

        Instant deadlineOf(String userId) {
            return offlineDeadlines.get(userId);
        }

        private boolean hasLiveSessions(String userId) {
            return !liveSessions.getOrDefault(userId, Set.of()).isEmpty();
        }

        private boolean isOnline(String userId) {
            return hasLiveSessions(userId) || offlineDeadlines.containsKey(userId);
        }
    }
}
