package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.BulkheadStateBackend;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 舱占用心跳后端（spec 127 / T473）：单 HASH 键承载全部心跳——
 * field = {@code agent|instance}，value = JSON（occupied / limit / at / expireAt）；
 * 键 TTL 兜底回收（活跃语义由条目 expireAt 惰性判定精确表达）。
 *
 * <p>故障语义：Redis 不可达 = 降级空快照（WARN 不上抛——观测面故障不放大为服务
 * 故障，spec 57 / spec 127 同款；恢复后心跳自愈）。
 */
public final class RedisBulkheadStateBackend implements BulkheadStateBackend, AutoCloseable {

    /** 惰性建连源（公开构造注入 client 时非 null；注入现成连接的构造路径为 null）。 */
    private final RedisClient client;
    private volatile StatefulRedisConnection<String, String> connection;
    private final String key;

    /**
     * @param client    Lettuce 客户端（本类独占派生连接；调用方拥有 client 生命周期）
     * @param keyPrefix 键前缀（空则 {@code buzhou:}；键 = prefix + {@code bulkheart}）
     */
    public RedisBulkheadStateBackend(RedisClient client, String keyPrefix) {
        // 惰性建连：后端不可达不在构造器炸——降级空快照语义由操作层 catch 统一表达
        this.client = client;
        this.connection = null;
        this.key = prefixOf(keyPrefix) + "bulkheart";
    }

    RedisBulkheadStateBackend(StatefulRedisConnection<String, String> connection, String keyPrefix) {
        this.client = null;
        this.connection = connection;
        this.key = prefixOf(keyPrefix) + "bulkheart";
    }

    private static String prefixOf(String keyPrefix) {
        return keyPrefix == null || keyPrefix.isBlank() ? "buzhou:" : keyPrefix;
    }

    /** 首次使用才建连（已建则复用；sync() 为廉价缓存桩）；建连失败按操作层降级语义处理。 */
    private RedisCommands<String, String> commands() {
        StatefulRedisConnection<String, String> conn = connection;
        if (conn == null) {
            synchronized (this) {
                if (connection == null) {
                    connection = client.connect();
                }
                conn = connection;
            }
        }
        return conn.sync();
    }

    /** 连接生命周期出口（宿主显式关闭；client.shutdown() 亦可覆盖）。 */
    public void close() {
        StatefulRedisConnection<String, String> conn = connection;
        if (conn != null) {
            conn.close();
        }
    }

    @Override
    public void heartbeat(InstanceOccupancy occupancy, Duration ttl) {
        if (occupancy == null || occupancy.agentName() == null || occupancy.instanceId() == null
                || occupancy.agentName().isBlank() || occupancy.instanceId().isBlank()
                || occupancy.agentName().contains("|") || occupancy.instanceId().contains("|")
                || occupancy.occupied() < 0 || occupancy.limit() <= 0 || ttl == null
                || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException(
                    "占用心跳参数非法（agent/instance 非空且不含 '|'，occupied>=0，limit>0，ttl 为正）");
        }
        try {
            long expireAt = System.currentTimeMillis() + ttl.toMillis();
            Map<String, Object> value = Map.of(
                    "occupied", occupancy.occupied(),
                    "limit", occupancy.limit(),
                    "at", occupancy.at().toEpochMilli(),
                    "expireAt", expireAt);
            commands().hset(key, occupancy.agentName() + "|" + occupancy.instanceId(),
                    RedisJson.write(value));
            // 键 TTL 兜底：最迟一个心跳窗后整键回收（无实例也回收）
            commands().expire(key, Math.max(1, ttl.toSeconds() * 2));
        } catch (RuntimeException e) {
            warnDegraded("heartbeat", e);
        }
    }

    @Override
    public Map<String, Integer> clusterOccupancy() {
        return aggregate(true);
    }

    @Override
    public Map<String, Integer> clusterInstances() {
        return aggregate(false);
    }

    @Override
    public String kind() {
        return "redis";
    }

    /** 一次 HGETALL 聚合：mode=true 求占用和 / false 求实例数（过期条目惰性剔除）。 */
    private Map<String, Integer> aggregate(boolean sumOccupancy) {
        try {
            Map<String, String> all = commands().hgetall(key);
            long now = System.currentTimeMillis();
            Map<String, Integer> out = new HashMap<>();
            for (Map.Entry<String, String> e : all.entrySet()) {
                Map<String, Object> value = RedisJson.readMap(e.getValue());
                if (value == null) {
                    continue;
                }
                if (((Number) value.getOrDefault("expireAt", 0L)).longValue() <= now) {
                    commands().hdel(key, e.getKey());
                    continue;
                }
                String agent = e.getKey().substring(0, e.getKey().indexOf('|'));
                out.merge(agent, sumOccupancy
                        ? ((Number) value.getOrDefault("occupied", 0)).intValue() : 1,
                        Integer::sum);
            }
            return Map.copyOf(out);
        } catch (RuntimeException e) {
            warnDegraded("aggregate", e);
            return Map.of();
        }
    }

    private static void warnDegraded(String op, RuntimeException e) {
        // 观测面降级：WARN 不上抛（License 后端 fail-fast 刻意不同——风险不对称）
        System.getLogger(RedisBulkheadStateBackend.class.getName())
                .log(System.Logger.Level.WARNING,
                        "bulkhead heartbeat backend degraded at {0}: {1}（降级空快照，恢复自愈）",
                        op, e.getMessage());
    }
}
