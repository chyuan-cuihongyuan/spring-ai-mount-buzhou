package io.github.chyuan_cuihongyuan.buzhou.observability.analytics;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * span 状态分布读数（spec 543 / T827——519 同包扩散；Prometheus label
 * 聚合思想）：按 kind × status 计数——「MODEL 错误集中还是 TOOL 错误
 * 集中」「各 kind 的量级分布」一屏可读。
 *
 * <p>诚实边界：只统计给定 spans 集（跨会话聚合归 OLAP 下游）。
 */
public final class SpanStatusDistribution {

    private SpanStatusDistribution() {
    }

    /**
     * kind × status 计数（status 大小写归一；外层按 kind 字典序、内层按
     * status 字典序——输出稳定）。
     */
    public static Map<String, Map<String, Long>> analyze(List<SpanRecord> spans) {
        if (spans == null) {
            throw new IllegalArgumentException("spans 必须非空");
        }
        Map<String, Map<String, Long>> out = new java.util.TreeMap<>();
        for (SpanRecord span : spans) {
            String kind = span.kind() == null ? "unknown" : span.kind().toUpperCase();
            String status = span.status() == null || span.status().isEmpty()
                    ? "UNSET" : span.status().toUpperCase();
            out.computeIfAbsent(kind, k -> new java.util.TreeMap<>())
                    .merge(status, 1L, Long::sum);
        }
        return out;
    }

    /** 便捷重载：单会话 spans 读（spansOfSession）。 */
    public static Map<String, Map<String, Long>> analyze(ObservabilityStore store, String sessionId) {
        return analyze(store.spansOfSession(sessionId));
    }
}
