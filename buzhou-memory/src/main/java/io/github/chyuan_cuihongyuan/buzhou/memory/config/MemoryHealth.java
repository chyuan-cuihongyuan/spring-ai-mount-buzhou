package io.github.chyuan_cuihongyuan.buzhou.memory.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;

import java.time.Instant;
import java.util.Map;

/**
 * memory 机制健康（impl-41 / spec 13 §T66）：核心职能 = 记忆台账（state/消息）可读写。
 * 探针 = SessionStateStore 一次性 put/get/delete 往返（健康专用 key，不留残留）；
 * <b>DOWN 仅当探针抛异常</b>（存储不可用 = 核心职能不可用）；机制未启用报 UNKNOWN。
 */
public final class MemoryHealth implements BuzhouHealth {

    static final String PROBE_SESSION = "buzhou-health-probe";
    static final String PROBE_KEY = "probe";

    private final boolean enabled;
    private final BuzhouStores stores;

    public MemoryHealth(boolean enabled, BuzhouStores stores) {
        this.enabled = enabled;
        this.stores = stores;
    }

    @Override
    public String mechanism() {
        return "memory";
    }

    @Override
    public Status status() {
        if (!enabled) {
            return Status.UNKNOWN;
        }
        try {
            stores.sessionStateStore().put(PROBE_SESSION,
                    new StateEntry(PROBE_KEY, "ok", "buzhou-health", 0, null, Instant.now()));
            boolean readable = stores.sessionStateStore()
                    .get(PROBE_SESSION, PROBE_KEY).isPresent();
            stores.sessionStateStore().delete(PROBE_SESSION, PROBE_KEY);
            return readable ? Status.UP : Status.DOWN;
        } catch (RuntimeException e) {
            return Status.DOWN;
        }
    }

    @Override
    public Map<String, Object> details() {
        return Map.of("enabled", enabled, "probe", "state-store-roundtrip");
    }
}
