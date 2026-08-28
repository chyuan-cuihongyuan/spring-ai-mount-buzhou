package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.CircuitBreakerStateBackend;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.time.Instant;
import java.util.Optional;

/**
 * Redis 共享熔断闸后端（spec 57 §A / T254）：TTL 键表达冷却窗——键存活 = 全实例 OPEN，
 * 过期 = 可探测（免后台清理；LiteLLM deployment cooldown 思想）。
 *
 * <p>键 {@code <prefix>cb:<模型净化名>}，值 {@code <trips>@<openedAtEpochMs>}，
 * {@code PEXPIRE cooldownMs}（毫秒级）。异常态（键存而无 TTL）按可探测处理——
 * 永不因残键把模型永久锁死在 OPEN。
 */
public final class RedisCircuitBreakerStateBackend implements CircuitBreakerStateBackend, AutoCloseable {

    private final StatefulRedisConnection<String, String> connection;
    private final String keyPrefix;
    private final RedisCommands<String, String> commands;

    /**
     * @param client    Lettuce 客户端（本类独占派生连接；调用方拥有 client 生命周期）
     * @param keyPrefix 键前缀（缺省 {@code buzhou:cb:}；与 store 同域隔离建议带 {@code cb:} 段）
     */
    public RedisCircuitBreakerStateBackend(RedisClient client, String keyPrefix) {
        this(client.connect(), keyPrefix);
    }

    RedisCircuitBreakerStateBackend(StatefulRedisConnection<String, String> connection, String keyPrefix) {
        this.connection = connection;
        this.keyPrefix = keyPrefix == null || keyPrefix.isBlank() ? "buzhou:cb:" : keyPrefix;
        this.commands = connection.sync();
    }

    /** 连接生命周期出口（宿主显式关闭；client.shutdown() 亦可覆盖）。 */
    public void close() {
        connection.close();
    }

    @Override
    public void recordTrip(String modelName, Instant openedAt, long cooldownMs, int consecutiveTrips) {
        String key = key(modelName);
        commands.set(key, consecutiveTrips + "@" + openedAt.toEpochMilli());
        commands.pexpire(key, Math.max(1, cooldownMs));
    }

    @Override
    public Optional<TripMarker> activeTrip(String modelName) {
        String key = key(modelName);
        String value = commands.get(key);
        if (value == null) {
            return Optional.empty();
        }
        Long pttl = commands.pttl(key);
        if (pttl == null || pttl <= 0) {
            return Optional.empty(); // 键无 TTL（异常态）/ 即刻到期：按可探测，不永久锁死
        }
        int trips = 0;
        long openedAtMs = System.currentTimeMillis();
        int sep = value.indexOf('@');
        try {
            if (sep > 0) {
                trips = Integer.parseInt(value.substring(0, sep));
                openedAtMs = Long.parseLong(value.substring(sep + 1));
            }
        } catch (NumberFormatException ignored) {
            // 值损坏：退默认（trips=0 / openedAt=now）——拒绝语义只依赖 TTL 剩余
        }
        return Optional.of(new TripMarker(Instant.ofEpochMilli(openedAtMs), pttl, trips));
    }

    @Override
    public void clear(String modelName) {
        commands.del(key(modelName));
    }

    @Override
    public String kind() {
        return "redis";
    }

    private String key(String modelName) {
        String sanitized = modelName == null ? "unknown"
                : modelName.replaceAll("[^A-Za-z0-9._-]", "_");
        return keyPrefix + sanitized;
    }
}
