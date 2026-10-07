package io.github.quizup.profile.infrastructure.out.redis;

import io.github.quizup.profile.domain.model.PresenceRules;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'intégration des scripts Lua de leases de présence sur un Redis réel (Testcontainers) :
 * transitions, grâce, réclamation hors ligne atomique et heartbeat batch.
 */
class PresenceLeaseRedisAdapterTest {

    private static final String SESSION_KEY_PREFIX = "presence:session:";
    private static final String STATUS_KEY_PREFIX = "presence:user:status:";
    private static final String LIVENESS_KEY = "presence:liveness";

    private static GenericContainer<?> redisContainer;
    private static LettuceConnectionFactory connectionFactory;

    private StringRedisTemplate redis;
    private PresenceLeaseRedisAdapter adapter;

    @BeforeAll
    static void startRedis() {
        redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
        redisContainer.start();
        connectionFactory = new LettuceConnectionFactory(
                redisContainer.getHost(), redisContainer.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
    }

    @AfterAll
    static void stopRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        if (redisContainer != null) {
            redisContainer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        redis = new StringRedisTemplate(connectionFactory);
        redis.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
        adapter = new PresenceLeaseRedisAdapter(redis);
    }

    @Test
    void openSession_reportsTransitionOnlyOnce() {
        assertThat(adapter.openSession("u1", "s1")).isTrue();

        assertThat(adapter.openSession("u1", "s1")).isFalse();
        assertThat(redis.hasKey(STATUS_KEY_PREFIX + "u1")).isTrue();
    }

    @Test
    void closeLastSession_armsGrace_thenClaimBasculesOffline() {
        adapter.openSession("u1", "s1");

        assertThat(adapter.closeSession("u1", "s1")).isFalse();
        assertThat(redis.opsForZSet().score(LIVENESS_KEY, "u1")).isNotNull();

        forceDue("u1");

        assertThat(adapter.claimDueOffline(Instant.now())).containsExactly("u1");
        assertThat(redis.hasKey(STATUS_KEY_PREFIX + "u1")).isFalse();
        assertThat(redis.hasKey(SESSION_KEY_PREFIX + "s1")).isFalse();
    }

    @Test
    void closeOneOfTwoSessions_keepsPlayerAlive_evenIfDue() {
        adapter.openSession("u1", "s1");
        adapter.openSession("u1", "s2");

        assertThat(adapter.closeSession("u1", "s1")).isTrue();
        forceDue("u1");

        assertThat(adapter.claimDueOffline(Instant.now())).isEmpty();
        assertThat(redis.hasKey(STATUS_KEY_PREFIX + "u1")).isTrue();
    }

    @Test
    void reconnectBeforeClaim_cancelsOffline() {
        adapter.openSession("u1", "s1");
        adapter.closeSession("u1", "s1");
        forceDue("u1");

        adapter.openSession("u1", "s2");

        assertThat(adapter.claimDueOffline(Instant.now())).isEmpty();
        assertThat(redis.hasKey(STATUS_KEY_PREFIX + "u1")).isTrue();
    }

    @Test
    void renewSessions_extendsOnlyKnownSessions() {
        adapter.openSession("u1", "s1");

        adapter.renewSessions(List.of("s1", "ghost"));

        Long ttlMillis = redis.getExpire(SESSION_KEY_PREFIX + "s1", TimeUnit.MILLISECONDS);
        assertThat(ttlMillis).isGreaterThan(PresenceRules.SESSION_LEASE_TTL.toMillis() - 5_000);
        assertThat(redis.hasKey(SESSION_KEY_PREFIX + "ghost")).isFalse();
        assertThat(redis.opsForZSet().score(LIVENESS_KEY, "u1"))
                .isGreaterThan((double) System.currentTimeMillis());
    }

    private void forceDue(String userId) {
        redis.opsForZSet().add(LIVENESS_KEY, userId, System.currentTimeMillis() - 1_000);
    }
}
