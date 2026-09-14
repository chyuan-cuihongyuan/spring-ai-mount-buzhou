package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 会话 Span 树拓扑读面（spec 1414 / T2129 / impl 1067）——Jaeger DAG 依赖图
 * 思想（结构形状独立于时延）：时延维已有 TurnLatencyPercentiles/ToolGraphAnalyzer
 * （火焰图 timings），本面回答<b>结构</b>问题——「这轮嵌套多深、单点扇出多大、
 * 都在调哪类 span」。深度失控（工具递归循环）与扇出爆炸（并行工具风暴）各是
 * 不同的病灶，时延分布看不出来。
 *
 * <p>纯函数零 IO：吃 {@link SpanRecord} 列表（同一会话或任意 span 集合）。
 * 深度 = 根→叶最长节点数（根=1）；扇出 = 单节点最大子数；孤儿（父缺失）与
 * SpanParentIntegrityAudit 同判（parent 引用不在集合内）但只计数不列明细；
 * 环防护：父引用成环时按环成员节点计 1 层、不无限递归（诚实入档）。
 */
public final class SpanTreeTopology {

    private SpanTreeTopology() {
    }

    /**
     * @param totalSpans   span 总数
     * @param rootCount    根数（parent 空/空白）
     * @param orphanCount  孤儿数（父引用不在集合内——跨导出截断的正常形态）
     * @param maxDepth     根→叶最长节点数（根=1；空集合 0）
     * @param maxFanout    单节点最大子数
     * @param kindHistogram span kind 计数（数量降序、平名典序——「在调哪类」第一眼）
     */
    public record Topology(int totalSpans, int rootCount, int orphanCount,
                           int maxDepth, int maxFanout, Map<String, Integer> kindHistogram) {
    }

    /** 拓扑分析入口（无序容忍；按 spanId 建索引）。 */
    public static Topology analyze(List<SpanRecord> spans) {
        if (spans == null || spans.isEmpty()) {
            return new Topology(0, 0, 0, 0, 0, Map.of());
        }
        Map<String, List<SpanRecord>> byParent = new HashMap<>();
        Map<String, SpanRecord> byId = new HashMap<>();
        int roots = 0;
        for (SpanRecord span : spans) {
            byId.put(span.spanId(), span);
            String parent = span.parentSpanId();
            if (parent == null || parent.isBlank()) {
                roots++;
            } else {
                byParent.computeIfAbsent(parent, k -> new java.util.ArrayList<>()).add(span);
            }
        }
        int orphans = 0;
        int maxFanout = 0;
        for (Map.Entry<String, List<SpanRecord>> e : byParent.entrySet()) {
            if (!byId.containsKey(e.getKey())) {
                orphans += e.getValue().size();
            }
            maxFanout = Math.max(maxFanout, e.getValue().size());
        }
        int maxDepth = 0;
        Set<String> visited = new HashSet<>();
        for (SpanRecord span : spans) {
            if (span.parentSpanId() == null || span.parentSpanId().isBlank()) {
                maxDepth = Math.max(maxDepth, depthOf(span, byParent, visited, 1));
            }
        }
        Map<String, Integer> kinds = new LinkedHashMap<>();
        spans.stream().map(s -> s.kind() == null ? "UNKNOWN" : s.kind())
                .collect(java.util.stream.Collectors.groupingBy(k -> k,
                        java.util.stream.Collectors.summingInt(k -> 1)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .forEachOrdered(e -> kinds.put(e.getKey(), e.getValue()));
        return new Topology(spans.size(), roots, orphans, maxDepth, maxFanout,
                java.util.Collections.unmodifiableMap(kinds));
    }

    /** 迭代式深度（环防护：visited 命中按 0 层收敛）。 */
    private static int depthOf(SpanRecord node, Map<String, List<SpanRecord>> byParent,
                               Set<String> visited, int level) {
        if (!visited.add(node.spanId())) {
            return 0; // 环成员（诚实入档：环不计深）
        }
        List<SpanRecord> children = byParent.get(node.spanId());
        int best = level;
        if (children != null) {
            for (SpanRecord child : children) {
                best = Math.max(best, depthOf(child, byParent, visited, level + 1));
            }
        }
        visited.remove(node.spanId());
        return best;
    }
}
