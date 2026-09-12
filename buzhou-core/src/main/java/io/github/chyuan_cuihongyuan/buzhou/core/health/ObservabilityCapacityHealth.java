package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryObservabilityStore;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 内存观测库容量健康面（spec 729 / T1058，729 容量读数接线，548 同型）：
 * mechanism=memory-observability 恒 UP——逐出是容量压力数据非进程故障；
 * details：used/max/utilization/evicted。utilization=1 且 evicted 增长 =
 * 观测数据在被覆盖丢弃（告警归 312 订阅自裁）。
 */
public final class ObservabilityCapacityHealth implements BuzhouHealth {

    private final InMemoryObservabilityStore store;

    public ObservabilityCapacityHealth(InMemoryObservabilityStore store) {
        if (store == null) {
            throw new IllegalArgumentException("store 必须非空");
        }
        this.store = store;
    }

    @Override
    public String mechanism() {
        return "memory-observability";
    }

    @Override
    public Status status() {
        return Status.UP;
    }

    @Override
    public Map<String, Object> details() {
        Map<String, Object> details = new LinkedHashMap<>();
        int used = store.sessionCount();
        int max = store.maxSessions();
        details.put("used", (long) used);
        details.put("max", (long) max);
        details.put("utilization", max == 0 ? 1.0 : (double) used / max);
        details.put("evicted", store.evictedSessionCount());
        return details;
    }
}
