package io.github.quizup.profile.infrastructure.out.redis;

import io.github.quizup.profile.domain.model.PresenceRules;
import io.github.quizup.profile.domain.port.out.PresenceLeasePort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptateur Redis des leases de présence (source de vérité {@code ONLINE}/{@code OFFLINE}).
 *
 * <p>Clés : {@code presence:session:{sessionId}} (lease TTL, valeur = userId),
 * {@code presence:user:sessions:{userId}} (ensemble des sessions),
 * {@code presence:user:status:{userId}} (état chaud à TTL) et {@code presence:liveness}
 * (ZSET des échéances de reprise). Les transitions sont atomiques (scripts Lua) : un joueur
 * n'est réclamé hors ligne que par une seule instance.</p>
 *
 * <p>Le déploiement Redis actuel est mono-instance (pas de Redis Cluster) ; les scripts
 * manipulent plusieurs clés, ce qui resterait à revoir (hash tags) en cas de sharding.</p>
 */
@Component
public class PresenceLeaseRedisAdapter implements PresenceLeasePort {

    private static final String SESSION_PREFIX = "presence:session:";
    private static final String USER_SESSIONS_PREFIX = "presence:user:sessions:";
    private static final String USER_STATUS_PREFIX = "presence:user:status:";
    private static final String LIVENESS_KEY = "presence:liveness";

    /** Taille de lot du heartbeat (borne la taille des scripts Lua et des paquets réseau). */
    private static final int RENEW_CHUNK_SIZE = 500;

    /** Nombre d'échéances traitées par balayage. */
    private static final int CLAIM_BATCH_SIZE = 1000;

    private final StringRedisTemplate redis;

    public PresenceLeaseRedisAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean openSession(String userId, String sessionId) {
        long now = System.currentTimeMillis();
        Long statusExisted = redis.execute(OPEN_SCRIPT,
                List.of(USER_STATUS_PREFIX + userId, USER_SESSIONS_PREFIX + userId,
                        SESSION_PREFIX + sessionId, LIVENESS_KEY),
                userId,
                sessionId,
                String.valueOf(PresenceRules.SESSION_LEASE_TTL.toMillis()),
                String.valueOf(PresenceRules.ONLINE_LIVENESS.toMillis()),
                String.valueOf(now + PresenceRules.ONLINE_LIVENESS.toMillis()));
        return statusExisted != null && statusExisted == 0L;
    }

    @Override
    public boolean closeSession(String userId, String sessionId) {
        long deadline = System.currentTimeMillis() + PresenceRules.DISCONNECT_GRACE.toMillis();
        Long alive = redis.execute(CLOSE_SCRIPT,
                List.of(SESSION_PREFIX + sessionId, USER_SESSIONS_PREFIX + userId,
                        LIVENESS_KEY, SESSION_PREFIX),
                userId,
                sessionId,
                String.valueOf(deadline));
        return alive != null && alive == 1L;
    }

    @Override
    public void renewSessions(List<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }
        long deadline = System.currentTimeMillis() + PresenceRules.ONLINE_LIVENESS.toMillis();
        for (int offset = 0; offset < sessionIds.size(); offset += RENEW_CHUNK_SIZE) {
            List<String> chunk = sessionIds.subList(offset, Math.min(offset + RENEW_CHUNK_SIZE, sessionIds.size()));
            List<String> args = new ArrayList<>(chunk.size() + 3);
            args.add(String.valueOf(PresenceRules.SESSION_LEASE_TTL.toMillis()));
            args.add(String.valueOf(PresenceRules.ONLINE_LIVENESS.toMillis()));
            args.add(String.valueOf(deadline));
            args.addAll(chunk);
            redis.execute(RENEW_SCRIPT,
                    List.of(SESSION_PREFIX, USER_STATUS_PREFIX, LIVENESS_KEY),
                    args.toArray());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> claimDueOffline(Instant now) {
        long nowMillis = now.toEpochMilli();
        List<String> claimed = (List<String>) redis.execute(CLAIM_SCRIPT,
                List.of(LIVENESS_KEY, SESSION_PREFIX, USER_SESSIONS_PREFIX, USER_STATUS_PREFIX),
                String.valueOf(nowMillis),
                String.valueOf(CLAIM_BATCH_SIZE),
                String.valueOf(PresenceRules.ONLINE_LIVENESS.toMillis()),
                String.valueOf(nowMillis + PresenceRules.ONLINE_LIVENESS.toMillis()));
        return claimed == null ? List.of() : claimed;
    }

    private static final String OPEN_LUA = """
            local statusKey = KEYS[1]
            local userSessionsKey = KEYS[2]
            local sessionKey = KEYS[3]
            local livenessKey = KEYS[4]
            local userId = ARGV[1]
            local sessionId = ARGV[2]
            local sessionTtl = tonumber(ARGV[3])
            local userTtl = tonumber(ARGV[4])
            local deadline = tonumber(ARGV[5])

            local wasOnline = redis.call('EXISTS', statusKey)
            redis.call('SET', sessionKey, userId, 'PX', sessionTtl)
            redis.call('SADD', userSessionsKey, sessionId)
            redis.call('PEXPIRE', userSessionsKey, sessionTtl + userTtl)
            redis.call('SET', statusKey, 'ONLINE', 'PX', userTtl)
            redis.call('ZADD', livenessKey, deadline, userId)
            return wasOnline
            """;

    private static final String CLOSE_LUA = """
            local sessionKey = KEYS[1]
            local userSessionsKey = KEYS[2]
            local livenessKey = KEYS[3]
            local sessionPrefix = KEYS[4]
            local userId = ARGV[1]
            local sessionId = ARGV[2]
            local graceDeadline = tonumber(ARGV[3])

            redis.call('DEL', sessionKey)
            redis.call('SREM', userSessionsKey, sessionId)

            local members = redis.call('SMEMBERS', userSessionsKey)
            local alive = false
            for i, member in ipairs(members) do
                if redis.call('EXISTS', sessionPrefix .. member) == 1 then
                    alive = true
                    break
                end
            end

            if not alive then
                redis.call('ZADD', livenessKey, graceDeadline, userId)
            end
            return alive and 1 or 0
            """;

    private static final String RENEW_LUA = """
            local sessionPrefix = KEYS[1]
            local statusPrefix = KEYS[2]
            local livenessKey = KEYS[3]
            local sessionTtl = tonumber(ARGV[1])
            local userTtl = tonumber(ARGV[2])
            local deadline = tonumber(ARGV[3])

            local renewed = 0
            for i = 4, #ARGV do
                local sessionKey = sessionPrefix .. ARGV[i]
                local userId = redis.call('GET', sessionKey)
                if userId then
                    redis.call('PEXPIRE', sessionKey, sessionTtl)
                    redis.call('SET', statusPrefix .. userId, 'ONLINE', 'PX', userTtl)
                    redis.call('ZADD', livenessKey, deadline, userId)
                    renewed = renewed + 1
                end
            end
            return renewed
            """;

    private static final String CLAIM_LUA = """
            local livenessKey = KEYS[1]
            local sessionPrefix = KEYS[2]
            local userSessionsPrefix = KEYS[3]
            local statusPrefix = KEYS[4]
            local now = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])
            local userTtl = tonumber(ARGV[3])
            local nextDeadline = tonumber(ARGV[4])

            local due = redis.call('ZRANGEBYSCORE', livenessKey, '-inf', now, 'LIMIT', 0, limit)
            local offline = {}
            for i, userId in ipairs(due) do
                local members = redis.call('SMEMBERS', userSessionsPrefix .. userId)
                local alive = false
                for j, sessionId in ipairs(members) do
                    if redis.call('EXISTS', sessionPrefix .. sessionId) == 1 then
                        alive = true
                        break
                    end
                end
                if alive then
                    redis.call('SET', statusPrefix .. userId, 'ONLINE', 'PX', userTtl)
                    redis.call('ZADD', livenessKey, nextDeadline, userId)
                else
                    redis.call('ZREM', livenessKey, userId)
                    redis.call('DEL', statusPrefix .. userId)
                    table.insert(offline, userId)
                end
            end
            return offline
            """;

    private static final RedisScript<Long> OPEN_SCRIPT = new DefaultRedisScript<>(OPEN_LUA, Long.class);
    private static final RedisScript<Long> CLOSE_SCRIPT = new DefaultRedisScript<>(CLOSE_LUA, Long.class);
    private static final RedisScript<Long> RENEW_SCRIPT = new DefaultRedisScript<>(RENEW_LUA, Long.class);
    private static final RedisScript<List> CLAIM_SCRIPT = new DefaultRedisScript<>(CLAIM_LUA, List.class);
}
