package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.BulkheadStateBackend;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内舱占用心跳后端（spec 127 / T473）：单进程部署 / 测试默认——
 * ConcurrentHashMap 心跳表 + 读时惰性过期清扫（无后台线程）。
 *
 * <p>key = {@code agent|instance}（与 Redis 实现编码一致）。
 */
public final class InMemoryBulkheadStateBackend implements BulkheadStateBackend {

    private record Beat(int occupied, long expireAtMillis) {
    }

    private final Map<String, Beat> beats = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryBulkheadStateBackend() {
        this(Clock.systemUTC());
    }

    /** 测试时钟注入（TTL 过期断言用）。 */
    public InMemoryBulkheadStateBackend(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void heartbeat(InstanceOccupancy occupancy, Duration ttl) {
        requireValid(occupancy, ttl);
        beats.put(field(occupancy.agentName(), occupancy.instanceId()),
                new Beat(occupancy.occupied(),
                        clock.millis() + ttl.toMillis()));
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
        return "in-memory";
    }

    /** 聚合活跃心跳：mode=true 求占用和、false 求实例数（同一次惰性清扫）。 */
    private Map<String, Integer> aggregate(boolean sumOccupancy) {
        long now = clock.millis();
        Map<String, Integer> out = new HashMap<>();
        beats.forEach((field, beat) -> {
            if (beat.expireAtMillis() <= now) {
                beats.remove(field);
                return;
            }
            String agent = agentOf(field);
            out.merge(agent, sumOccupancy ? beat.occupied() : 1, Integer::sum);
        });
        return Map.copyOf(out);
    }

    private static String field(String agent, String instance) {
        return agent + "|" + instance;
    }

    private static String agentOf(String field) {
        return field.substring(0, field.indexOf('|'));
    }

    private static void requireValid(InstanceOccupancy occupancy, Duration ttl) {
        if (occupancy == null || occupancy.agentName() == null || occupancy.instanceId() == null
                || occupancy.agentName().isBlank() || occupancy.instanceId().isBlank()
                || occupancy.agentName().contains("|") || occupancy.instanceId().contains("|")
                || occupancy.occupied() < 0 || occupancy.limit() <= 0 || ttl == null
                || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException(
                    "占用心跳参数非法（agent/instance 非空且不含 '|'，occupied>=0，limit>0，ttl 为正）");
        }
    }
}
