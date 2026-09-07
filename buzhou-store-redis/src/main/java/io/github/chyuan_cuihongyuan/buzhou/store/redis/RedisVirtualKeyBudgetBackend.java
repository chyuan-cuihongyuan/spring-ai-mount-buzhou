package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.VirtualKeyBudgetBackend;
import io.lettuce.core.RedisClient;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * Redis 共享虚拟 key 配额后端（spec 315 / T622，Redisson 分布式限额器借鉴）：
 * Lua 原子扣减——{@code spent + δ ≤ limit} 才 INCRBY（check-then-incr 一体，
 * 竞态窗口零）。键 {@code <prefix>vk:<key>} 值 = 已用 tokens；DEL 清零。
 * 后端异常上抛（VirtualKeys 层 fail-closed——配额面宁可拒绝不可超支）。
 */
public final class RedisVirtualKeyBudgetBackend implements VirtualKeyBudgetBackend, AutoCloseable {

    private static final String SPEND_LUA = """
            local cur = tonumber(redis.call('GET', KEYS[1]) or '0')
            if cur + tonumber(ARGV[1]) <= tonumber(ARGV[2]) then
              redis.call('INCRBY', KEYS[1], ARGV[1])
              return 1
            end
            return 0""";

    private final StatefulRedisConnection<String, String> connection;
    private final String keyPrefix;
    private final RedisCommands<String, String> commands;

    /**
     * @param client    Lettuce 客户端（本类独占派生连接；调用方拥有 client 生命周期）
     * @param keyPrefix 键前缀（缺省 {@code buzhou:vk:}）
     */
    public RedisVirtualKeyBudgetBackend(RedisClient client, String keyPrefix) {
        this(client.connect(), keyPrefix);
    }

    RedisVirtualKeyBudgetBackend(StatefulRedisConnection<String, String> connection, String keyPrefix) {
        this.connection = connection;
        this.keyPrefix = keyPrefix == null || keyPrefix.isBlank() ? "buzhou:vk:" : keyPrefix;
        this.commands = connection.sync();
    }

    @Override
    public boolean trySpend(String key, long tokens, long limitTokens) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (tokens <= 0) {
            return true; // 零扣减恒成功（与 VirtualKeys 口径一致）
        }
        Long result = commands.eval(SPEND_LUA, ScriptOutputType.INTEGER,
                new String[]{keyOf(key)},
                String.valueOf(tokens), String.valueOf(limitTokens));
        return result != null && result == 1L;
    }

    @Override
    public long usedTokens(String key) {
        String value = commands.get(keyOf(key));
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L; // 脏值按零（观测面不炸）
        }
    }

    @Override
    public void reset(String key) {
        commands.del(keyOf(key));
    }

    private String keyOf(String key) {
        return keyPrefix + key;
    }

    /** 连接生命周期出口（宿主显式关闭；client.shutdown() 亦可覆盖）。 */
    public void close() {
        connection.close();
    }
}
