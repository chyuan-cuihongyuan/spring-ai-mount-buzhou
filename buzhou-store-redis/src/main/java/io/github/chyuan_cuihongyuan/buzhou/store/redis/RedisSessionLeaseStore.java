package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaseAcquireResult;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaseInfo;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionLeaseStore;
import io.lettuce.core.ScriptOutputType;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Redis {@link SessionLeaseStore}：租约存 HASH（owner/fencingToken/acquiredAt/expiresAt），
 * key 经 {@code PEXPIRE} 设毫秒 TTL——租约到期由 Redis 自动删除，{@code tryAcquire} 的 EXISTS 判定
 * 即「是否被占用」，天然覆盖自然到期语义。
 *
 * <p>fencing token 经独立计数器 {@code INCR}（无 TTL，单调不复用）；acquire/steal 在同一 Lua 内
 * 「判定 → INCR → 写入 → PEXPIRE」原子完成；renew/release 经 owner+fencingToken 双校验。
 */
public class RedisSessionLeaseStore implements SessionLeaseStore {

    /** acquire：EXISTS 即占用失败返回 0；否则 INCR 取 token、写 lease、PEXPIRE，返回 token(≥1)。 */
    private static final String ACQUIRE_SCRIPT = """
            if redis.call('EXISTS', KEYS[1]) == 1 then return 0 end
            local token = redis.call('INCR', KEYS[2])
            redis.call('HSET', KEYS[1], 'owner', ARGV[1], 'fencingToken', token,
                       'acquiredAt', ARGV[2], 'expiresAt', ARGV[3])
            redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[4]))
            return token""";

    /** renew：owner + fencingToken 双校验通过才 PEXPIRE 续期。 */
    private static final String RENEW_SCRIPT = """
            if redis.call('HGET', KEYS[1], 'owner') == ARGV[1]
               and redis.call('HGET', KEYS[1], 'fencingToken') == ARGV[2] then
              redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[3]))
              return 1
            end
            return 0""";

    /** steal：无条件 INCR 取新 token、覆写 owner、PEXPIRE。 */
    private static final String STEAL_SCRIPT = """
            local token = redis.call('INCR', KEYS[2])
            redis.call('HSET', KEYS[1], 'owner', ARGV[1], 'fencingToken', token,
                       'acquiredAt', ARGV[2], 'expiresAt', ARGV[3])
            redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[4]))
            return token""";

    /** release：owner + fencingToken 双校验通过才 DEL。 */
    private static final String RELEASE_SCRIPT = """
            if redis.call('HGET', KEYS[1], 'owner') == ARGV[1]
               and redis.call('HGET', KEYS[1], 'fencingToken') == ARGV[2] then
              redis.call('DEL', KEYS[1])
              return 1
            end
            return 0""";

    private final RedisSync sync;
    private final RedisKeys keys;

    public RedisSessionLeaseStore(RedisSync sync, RedisKeys keys) {
        this.sync = sync;
        this.keys = keys;
    }

    @Override
    public LeaseAcquireResult tryAcquire(String sessionId, String ownerId, Duration ttl) {
        Instant now = Instant.now();
        long token = sync.commands().eval(ACQUIRE_SCRIPT, ScriptOutputType.INTEGER,
                new String[]{keys.lease(sessionId), keys.leaseFencingSeq(sessionId)},
                ownerId, now.toString(), now.plus(ttl).toString(), Long.toString(ttl.toMillis()));
        return new LeaseAcquireResult(token >= 1L, Math.max(0L, token));
    }

    @Override
    public boolean renew(String sessionId, String ownerId, long fencingToken, Duration ttl) {
        Long ok = sync.commands().eval(RENEW_SCRIPT, ScriptOutputType.INTEGER,
                new String[]{keys.lease(sessionId)},
                ownerId, Long.toString(fencingToken), Long.toString(ttl.toMillis()));
        return ok != null && ok == 1L;
    }

    @Override
    public void release(String sessionId, String ownerId, long fencingToken) {
        sync.commands().eval(RELEASE_SCRIPT, ScriptOutputType.INTEGER,
                new String[]{keys.lease(sessionId)},
                ownerId, Long.toString(fencingToken));
    }

    @Override
    public LeaseAcquireResult steal(String sessionId, String newOwnerId, Duration ttl) {
        Instant now = Instant.now();
        long token = sync.commands().eval(STEAL_SCRIPT, ScriptOutputType.INTEGER,
                new String[]{keys.lease(sessionId), keys.leaseFencingSeq(sessionId)},
                newOwnerId, now.toString(), now.plus(ttl).toString(), Long.toString(ttl.toMillis()));
        return new LeaseAcquireResult(true, token);
    }

    @Override
    public Optional<LeaseInfo> inspect(String sessionId) {
        // EXISTS 判定覆盖自然到期：TTL 到点 Redis 已删 key
        var c = sync.commands();
        String leaseKey = keys.lease(sessionId);
        if (c.exists(leaseKey) == 0L) {
            return Optional.empty();
        }
        Map<String, String> fields = c.hgetall(leaseKey);
        if (fields == null || fields.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new LeaseInfo(
                fields.get("owner"),
                Long.parseLong(fields.get("fencingToken")),
                Instant.parse(fields.get("acquiredAt")),
                Instant.parse(fields.get("expiresAt"))));
    }
}
