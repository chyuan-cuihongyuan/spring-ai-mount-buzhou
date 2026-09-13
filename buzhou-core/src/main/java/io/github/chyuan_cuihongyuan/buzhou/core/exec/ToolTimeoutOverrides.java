package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * per-tool 超时覆盖（spec 529 / T811，spec 31 结果限幅的 per-tool glob
 * 覆盖同法 × 单工具超时（impl-28）扩展）：glob 通配键 → 覆盖毫秒值。
 * 覆盖生效时 = min(覆盖值, Deadline 剩余)——全局 toolTimeout 被替换；
 * 无覆盖 = 全局值零变化。默认空表（Holder 模式——零默认行为变化）。
 */
public final class ToolTimeoutOverrides {

    private final Map<String, Long> overrides;
    /** impl-768 / spec 1015：命中读面（按配置模式分桶——0 = 幽灵覆盖）。 */
    private final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicLong> hitsByPattern =
            new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong lookups = new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong hits = new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong misses = new java.util.concurrent.atomic.AtomicLong();

    public ToolTimeoutOverrides(Map<String, Long> overrides) {
        if (overrides != null) {
            overrides.values().forEach(v -> {
                if (v == null || v < -1) {
                    throw new IllegalArgumentException("超时覆盖值须 >= -1（-1 = 用全局）");
                }
            });
        }
        this.overrides = overrides == null ? Map.of() : Map.copyOf(overrides);
        this.overrides.keySet().forEach(p ->
                hitsByPattern.put(p, new java.util.concurrent.atomic.AtomicLong()));
    }

    /** 全局默认档（无覆盖——全局 toolTimeout 生效）。 */
    public static ToolTimeoutOverrides disabled() {
        return new ToolTimeoutOverrides(Map.of());
    }

    /** per-tool 生效覆盖毫秒（glob 首个命中；-1 = 用全局；无覆盖 = -1）。 */
    public long timeoutMillisFor(String toolName) {
        lookups.incrementAndGet();
        for (Map.Entry<String, Long> e : overrides.entrySet()) {
            if (globMatch(e.getKey(), toolName)) {
                hits.incrementAndGet();
                hitsByPattern.get(e.getKey()).incrementAndGet();
                return e.getValue();
            }
        }
        misses.incrementAndGet();
        return -1;
    }

    /** 命中读面快照（spec 1015——hitsByPattern 含全部配置模式，0 = 幽灵覆盖）。 */
    public ToolTimeoutOverrideStats stats() {
        java.util.Map<String, Long> byPattern = new java.util.LinkedHashMap<>();
        hitsByPattern.forEach((k, v) -> byPattern.put(k, v.get()));
        return new ToolTimeoutOverrideStats(lookups.get(), hits.get(),
                misses.get(), java.util.Collections.unmodifiableMap(byPattern));
    }

    /** 极简 glob：{@code *} 通配任意串（31 同法）。 */
    static boolean globMatch(String pattern, String name) {
        int star = pattern.indexOf('*');
        if (star < 0) {
            return pattern.equals(name);
        }
        String prefix = pattern.substring(0, star);
        String suffix = pattern.substring(star + 1);
        if (!name.startsWith(prefix) || !name.endsWith(suffix)) {
            return false;
        }
        return name.length() >= prefix.length() + suffix.length();
    }

    /**
     * 全局默认 Holder（{@link ToolResultLimiterHolder} 同型）。
     */
    public static final class Holder {

        private static final AtomicReference<ToolTimeoutOverrides> CURRENT =
                new AtomicReference<>(ToolTimeoutOverrides.disabled());

        private Holder() {
        }

        public static ToolTimeoutOverrides current() {
            return CURRENT.get();
        }

        public static void set(ToolTimeoutOverrides overrides) {
            CURRENT.set(overrides == null ? ToolTimeoutOverrides.disabled() : overrides);
        }
    }
}
