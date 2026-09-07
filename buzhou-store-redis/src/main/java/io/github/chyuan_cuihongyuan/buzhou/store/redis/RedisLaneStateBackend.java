package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.LaneStateBackend;
import io.lettuce.core.RedisClient;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * Redis 共享泳道许可后端（spec 316 / T624，Redisson 分布式信号量语义收窄）：
 * Lua 原子取/还——acquire：{@code cur+1 ≤ permits} 才 INCR；release：DECR
 * floor-0（多还不欠）。键 {@code <prefix>lane:<名>} 值 = 在取许可数。
 * 崩溃泄漏靠 {@link #reset(String)} 运维清零（诚实边界入档 spec 316）。
 */
public final class RedisLaneStateBackend implements LaneStateBackend, AutoCloseable {

    private static final String ACQUIRE_LUA = """
            local cur = tonumber(redis.call('GET', KEYS[1]) or '0')
            if cur + 1 <= tonumber(ARGV[1]) then
              redis.call('INCR', KEYS[1])
              return 1
            end
            return 0""";

    private static final String RELEASE_LUA = """
            local cur = tonumber(redis.call('GET', KEYS[1]) or '0')
            if cur <= 1 then
              redis.call('DEL', KEYS[1])
              return 0
            end
            redis.call('DECR', KEYS[1])
            return cur - 1""";

    private final StatefulRedisConnection<String, String> connection;
    private final String keyPrefix;
    private final RedisCommands<String, String> commands;

    /**
     * @param client    Lettuce 客户端（本类独占派生连接；调用方拥有 client 生命周期）
     * @param keyPrefix 键前缀（缺省 {@code buzhou:lane:}）
     */
    public RedisLaneStateBackend(RedisClient client, String keyPrefix) {
        this(client.connect(), keyPrefix);
    }

    RedisLaneStateBackend(StatefulRedisConnection<String, String> connection, String keyPrefix) {
        this.connection = connection;
        this.keyPrefix = keyPrefix == null || keyPrefix.isBlank() ? "buzhou:lane:" : keyPrefix;
        this.commands = connection.sync();
    }

    @Override
    public boolean tryAcquire(String lane, int permits) {
        if (lane == null || lane.isBlank()) {
            throw new IllegalArgumentException("lane must not be blank");
        }
        Long result = commands.eval(ACQUIRE_LUA, ScriptOutputType.INTEGER,
                new String[]{keyOf(lane)}, String.valueOf(permits));
        return result != null && result == 1L;
    }

    @Override
    public void release(String lane) {
        commands.eval(RELEASE_LUA, ScriptOutputType.INTEGER, new String[]{keyOf(lane)});
    }

    @Override
    public void reset(String lane) {
        commands.del(keyOf(lane));
    }

    private String keyOf(String lane) {
        return keyPrefix + lane;
    }

    /** 连接生命周期出口。 */
    public void close() {
        connection.close();
    }
}
