package io.github.quizup.profile.application.service;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.profile.domain.event.PresenceEvent;
import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.domain.model.PresenceRules;
import io.github.quizup.profile.domain.model.PresenceStatus;
import io.github.quizup.profile.domain.port.out.PresenceRepositoryPort;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PresenceServiceTest {

    private InMemoryPresenceRepository repository;
    private EventGateway eventGateway;
    private PresenceService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPresenceRepository();
        eventGateway = mock(EventGateway.class);
        service = new PresenceService(repository, eventGateway);
    }

    @Test
    void firstSessionMarksPlayerOnlineAndPublishesOnlineEvent() {
        PlayerPresence presence = service.sessionConnected("s1", "u1", "bff:1");

        assertThat(presence.status()).isEqualTo(PresenceStatus.ONLINE);
        assertThat(presence.lastSeenAt()).isNotNull();
        assertThat(presence.offlineDeadlineAt()).isNull();
        verify(eventGateway, times(1)).publish(any(PresenceEvent.PlayerWentOnlineEvent.class));
    }

    @Test
    void additionalSessionDoesNotPublishOnlineEventAgain() {
        service.sessionConnected("s1", "u1", "bff:1");
        service.sessionConnected("s2", "u1", "bff:1");

        verify(eventGateway, times(1)).publish(any(PresenceEvent.PlayerWentOnlineEvent.class));
    }

    @Test
    void closingLastSessionArmsOfflineDeadlineAndStaysOnline() {
        service.sessionConnected("s1", "u1", "bff:1");

        PlayerPresence presence = service.sessionDisconnected("s1", "u1");

        assertThat(presence.status()).isEqualTo(PresenceStatus.ONLINE);
        assertThat(presence.lastSeenAt()).isNotNull();
        assertThat(presence.offlineDeadlineAt())
                .isAfter(Instant.now().plusSeconds(10))
                .isBefore(Instant.now().plus(PresenceRules.DISCONNECT_GRACE).plusSeconds(5));
    }

    @Test
    void closingSessionWithAnotherOpenSessionDoesNotArmDeadline() {
        service.sessionConnected("s1", "u1", "bff:1");
        service.sessionConnected("s2", "u1", "bff:1");

        service.sessionDisconnected("s1", "u1");

        assertThat(service.get("u1").offlineDeadlineAt()).isNull();
    }

    @Test
    void expiryTurnsPlayerOfflineAndPublishesEvent() {
        service.sessionConnected("s1", "u1", "bff:1");
        service.sessionDisconnected("s1", "u1");
        repository.forceDeadlineInThePast("u1");

        service.expireOfflineDeadlines();

        assertThat(service.get("u1").status()).isEqualTo(PresenceStatus.OFFLINE);
        verify(eventGateway).publish(any(PresenceEvent.PlayerWentOfflineEvent.class));
    }

    @Test
    void expiryIsNoOpWhenReconnected() {
        service.sessionConnected("s1", "u1", "bff:1");
        service.sessionDisconnected("s1", "u1");
        repository.forceDeadlineInThePast("u1");
        service.sessionConnected("s2", "u1", "bff:1");

        service.expireOfflineDeadlines();

        assertThat(service.get("u1").status()).isEqualTo(PresenceStatus.ONLINE);
        verify(eventGateway, never()).publish(any(PresenceEvent.PlayerWentOfflineEvent.class));
    }

    @Test
    void resetInstanceSessionsPurgesOnlyThatInstanceAndArmsDeadlines() {
        service.sessionConnected("s1", "u1", "bff:1");
        service.sessionConnected("s2", "u2", "bff:2");

        service.resetInstanceSessions("bff:1", Instant.now().plusSeconds(1));

        assertThat(repository.countSessions("u1")).isZero();
        assertThat(repository.countSessions("u2")).isEqualTo(1);
        assertThat(service.get("u1").offlineDeadlineAt()).isNotNull();
        assertThat(service.get("u2").offlineDeadlineAt()).isNull();
    }

    @Test
    void resetInstanceSessionsKeepsSessionsOpenedAfterStart() {
        service.sessionConnected("s1", "u1", "bff:1");

        service.resetInstanceSessions("bff:1", Instant.now().minusSeconds(1));

        assertThat(repository.countSessions("u1")).isEqualTo(1);
        assertThat(service.get("u1").offlineDeadlineAt()).isNull();
    }

    @Test
    void staleSessionIsPurgedAndArmsOfflineDeadline() {
        service.sessionConnected("s1", "u1", "bff:1");
        repository.ageSession("s1", PresenceRules.SESSION_LEASE_TTL.plusSeconds(10));

        service.expireStaleSessions();

        assertThat(repository.countSessions("u1")).isZero();
        assertThat(service.get("u1").status()).isEqualTo(PresenceStatus.ONLINE);
        assertThat(service.get("u1").offlineDeadlineAt()).isNotNull();

        repository.forceDeadlineInThePast("u1");
        service.expireOfflineDeadlines();

        assertThat(service.get("u1").status()).isEqualTo(PresenceStatus.OFFLINE);
        verify(eventGateway).publish(any(PresenceEvent.PlayerWentOfflineEvent.class));
    }

    @Test
    void renewedSessionSurvivesStaleSweep() {
        service.sessionConnected("s1", "u1", "bff:1");
        repository.ageSession("s1", PresenceRules.SESSION_LEASE_TTL.plusSeconds(10));

        service.renewSessions(List.of("s1"));
        service.expireStaleSessions();

        assertThat(repository.countSessions("u1")).isEqualTo(1);
        assertThat(service.get("u1").status()).isEqualTo(PresenceStatus.ONLINE);
        assertThat(service.get("u1").offlineDeadlineAt()).isNull();
    }

    @Test
    void staleSessionIsKeptWhenAnotherSessionIsAlive() {
        service.sessionConnected("s1", "u1", "bff:1");
        service.sessionConnected("s2", "u1", "bff:1");
        repository.ageSession("s1", PresenceRules.SESSION_LEASE_TTL.plusSeconds(10));

        service.expireStaleSessions();

        assertThat(repository.countSessions("u1")).isEqualTo(1);
        assertThat(service.get("u1").offlineDeadlineAt()).isNull();
    }

    @Test
    void unknownPlayerIsOffline() {
        assertThat(service.get("unknown").status()).isEqualTo(PresenceStatus.OFFLINE);
    }

    private static final class InMemoryPresenceRepository implements PresenceRepositoryPort {

        private final Map<String, PlayerPresence> presences = new HashMap<>();
        private final Map<String, String> sessionOwners = new HashMap<>();
        private final Map<String, String> sessionInstances = new HashMap<>();
        private final Map<String, Instant> sessionConnectedAt = new HashMap<>();
        private final Map<String, Instant> sessionLastSeenAt = new HashMap<>();

        void forceDeadlineInThePast(String userId) {
            presences.computeIfPresent(userId, (id, presence) ->
                    presence.toBuilder().offlineDeadlineAt(Instant.now().minusSeconds(1)).build());
        }

        void ageSession(String sessionId, java.time.Duration age) {
            sessionLastSeenAt.computeIfPresent(sessionId, (id, seen) -> Instant.now().minus(age));
        }

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

        @Override
        public void addSession(String sessionId, String userId, String instanceId) {
            Instant now = Instant.now();
            sessionOwners.put(sessionId, userId);
            sessionInstances.put(sessionId, instanceId);
            sessionConnectedAt.put(sessionId, now);
            sessionLastSeenAt.put(sessionId, now);
        }

        @Override
        public void removeSession(String sessionId) {
            sessionOwners.remove(sessionId);
            sessionInstances.remove(sessionId);
            sessionConnectedAt.remove(sessionId);
            sessionLastSeenAt.remove(sessionId);
        }

        @Override
        public long countSessions(String userId) {
            return sessionOwners.values().stream().filter(userId::equals).count();
        }

        @Override
        public void touchSessions(List<String> sessionIds, Instant now) {
            sessionIds.forEach(sessionId -> sessionLastSeenAt.computeIfPresent(sessionId, (id, seen) -> now));
        }

        @Override
        public List<String> findStaleSessionUserIds(Instant before) {
            return sessionLastSeenAt.entrySet().stream()
                    .filter(entry -> entry.getValue().isBefore(before))
                    .map(entry -> sessionOwners.get(entry.getKey()))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }

        @Override
        public void deleteStaleSessions(Instant before) {
            List<String> staleSessions = sessionLastSeenAt.entrySet().stream()
                    .filter(entry -> entry.getValue().isBefore(before))
                    .map(Map.Entry::getKey)
                    .toList();
            staleSessions.forEach(this::removeSession);
        }

        @Override
        public List<String> userIdsByInstanceBefore(String instanceId, Instant before) {
            return sessionInstances.entrySet().stream()
                    .filter(entry -> instanceId.equals(entry.getValue()))
                    .filter(entry -> sessionConnectedAt.get(entry.getKey()).isBefore(before))
                    .map(entry -> sessionOwners.get(entry.getKey()))
                    .distinct()
                    .toList();
        }

        @Override
        public void deleteSessionsByInstanceBefore(String instanceId, Instant before) {
            List<String> sessionIds = sessionInstances.entrySet().stream()
                    .filter(entry -> instanceId.equals(entry.getValue()))
                    .filter(entry -> sessionConnectedAt.get(entry.getKey()).isBefore(before))
                    .map(Map.Entry::getKey)
                    .toList();
            sessionIds.forEach(this::removeSession);
        }

        @Override
        public List<PlayerPresence> findDueOffline(Instant now) {
            return presences.values().stream()
                    .filter(presence -> presence.status() == PresenceStatus.ONLINE)
                    .filter(presence -> presence.offlineDeadlineAt() != null
                            && !presence.offlineDeadlineAt().isAfter(now))
                    .filter(presence -> countSessions(presence.userId()) == 0)
                    .toList();
        }

        @Override
        public boolean markOffline(String userId, Instant now) {
            PlayerPresence presence = presences.get(userId);
            if (presence == null
                    || presence.status() != PresenceStatus.ONLINE
                    || presence.offlineDeadlineAt() == null
                    || presence.offlineDeadlineAt().isAfter(now)
                    || countSessions(userId) > 0) {
                return false;
            }
            presences.put(userId, presence.toBuilder()
                    .status(PresenceStatus.OFFLINE)
                    .offlineDeadlineAt(null)
                    .build());
            return true;
        }
    }
}
