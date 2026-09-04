package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaderElector;
import io.lettuce.core.RedisClient;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Redis 共享选主后端（spec 331 / T653，K8s leader election / etcd lease
 * 借鉴）：双键——持有人键（PSETEX TTL 租约）+ 纪元计数键（无 TTL，跨重启
 * 单调——围栏语义诚实，旧持有人迟到续期不复活旧纪元）。
 *
 * <p>Lua 原子取/续：持有人匹配 → 续 TTL 纪元不变；空位 → INCR 纪元并
 * 占位；他人持有 → 跟随。resign 仅持有人匹配才 DEL（快速故障转移）。
 * 后端异常上抛（调用方按「本周期不执行」处理——失联宁可少做不可抢做）。
 */
public final class RedisLeaderElector implements LeaderElector, AutoCloseable {

    /** 取/续一体：ARGV = [holderId, ttlMillis]；返回 {leader(0/1), epoch}。 */
    private static final String ACQUIRE_OR_RENEW_LUA = """
            local holder = redis.call('GET', KEYS[1])
            if holder == ARGV[1] then
              redis.call('PSETEX', KEYS[1], ARGV[2], ARGV[1])
              return {1, tonumber(redis.call('GET', KEYS[2]) or '0')}
            end
            if not holder then
              local epoch = redis.call('INCR', KEYS[2])
              redis.call('PSETEX', KEYS[1], ARGV[2], ARGV[1])
              return {1, epoch}
            end
            return {0, tonumber(redis.call('GET', KEYS[2]) or '0')}""";

    /** 让位：仅持有人匹配才 DEL（他人的让位是 no-op）。 */
    private static final String RESIGN_LUA = """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
              redis.call('DEL', KEYS[1])
              return 1
            end
            return 0""";

    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> commands;
    private final String holderKey;
    private final String epochKey;
    private final String holderId;
    private final long ttlMillis;

    /**
     * @param client   Lettuce 客户端（本类独占派生连接；调用方拥有 client 生命周期）
     * @param scopeKey 选主域键名（如 {@code buzhou:leader:housekeeping}）
     * @param holderId 本实例身份（null/空 → 自动生成）
     * @param ttl      租约 TTL（须大于消费方执行周期，建议 2×）
     */
    public RedisLeaderElector(RedisClient client, String scopeKey, String holderId, Duration ttl) {
        this(client.connect(), scopeKey, holderId, ttl);
    }

    RedisLeaderElector(StatefulRedisConnection<String, String> connection, String scopeKey,
            String holderId, Duration ttl) {
        if (scopeKey == null || scopeKey.isBlank()) {
            throw new IllegalArgumentException("scopeKey 非空");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl 为正");
        }
        this.connection = connection;
        this.commands = connection.sync();
        this.holderKey = scopeKey;
        this.epochKey = scopeKey + ":epoch";
        this.holderId = holderId == null || holderId.isBlank()
                ? "buzhou-" + UUID.randomUUID() : holderId;
        this.ttlMillis = ttl.toMillis();
    }

    @Override
    public Leadership tryAcquireOrRenew() {
        List<Object> result = commands.eval(ACQUIRE_OR_RENEW_LUA, ScriptOutputType.MULTI,
                new String[]{holderKey, epochKey}, holderId, String.valueOf(ttlMillis));
        boolean leader = asLong(result, 0) == 1L;
        long epoch = asLong(result, 1);
        return new Leadership(leader ? holderId : holderOf(), epoch, leader);
    }

    @Override
    public void resign() {
        commands.eval(RESIGN_LUA, ScriptOutputType.INTEGER,
                new String[]{holderKey}, holderId);
    }

    @Override
    public Leadership inspect() {
        return new Leadership(holderOf(), epochOf(), holderId.equals(holderOf()));
    }

    private String holderOf() {
        String value = commands.get(holderKey);
        return value == null ? null : value;
    }

    private long epochOf() {
        String value = commands.get(epochKey);
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L; // 脏值按零（观测面不炸）
        }
    }

    private static long asLong(List<Object> values, int index) {
        if (values == null || index >= values.size() || values.get(index) == null) {
            return 0L;
        }
        Object value = values.get(index);
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** 本实例身份（观测面）。 */
    public String holderId() {
        return holderId;
    }

    /** 连接生命周期出口（宿主显式关闭；client.shutdown() 亦可覆盖）。 */
    public void close() {
        connection.close();
    }
}
