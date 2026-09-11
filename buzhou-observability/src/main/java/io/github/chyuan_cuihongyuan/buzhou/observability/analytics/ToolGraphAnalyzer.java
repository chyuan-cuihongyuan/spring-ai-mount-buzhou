package io.github.chyuan_cuihongyuan.buzhou.observability.analytics;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具调用图谱统计（spec 519 / T789，LangSmith trace analytics 思想）：
 * TOOL span 按 (sessionId, turnSeq, startedAt) 排序——同轮相邻对生成有向
 * 边 toolA→toolB 计数 + per-tool calls/errors/错误率。「哪些工具总被连着
 * 用」「哪个工具错误集中」的读数面（prompt 工程/熔断配置依据）。
 *
 * <p>诚实边界：跨轮不连边（会话内时序链不做跨轮假设）；只在给定 spans
 * 集内统计（跨会话聚合归 OLAP JSONL 下游）。
 */
public final class ToolGraphAnalyzer {

    /** 有向边（from→to 及出现次数）。 */
    public record Edge(String from, String to, long count) {
    }

    /** 单工具汇总（calls/errors/错误率——0 除约定 0.0）。 */
    public record ToolTotal(String tool, long calls, long errors, double errorRate) {
    }

    /** 图谱报告（edges 按 count 降序、tools 按 calls 降序——同值字典序稳定）。 */
    public record ToolGraphReport(List<Edge> edges, List<ToolTotal> tools) {
    }

    private ToolGraphAnalyzer() {
    }

    /** 便捷重载：单会话 spans 读（spansOfSession）。 */
    public static ToolGraphReport analyze(ObservabilityStore store, String sessionId) {
        return analyze(store.spansOfSession(sessionId));
    }

    /** 纯函数分析：过滤 TOOL → 分组排序 → 相邻边 + 工具汇总。 */
    public static ToolGraphReport analyze(List<SpanRecord> spans) {
        if (spans == null) {
            throw new IllegalArgumentException("spans 必须非空");
        }
        List<SpanRecord> toolSpans = spans.stream()
                .filter(s -> s.kind() != null && s.kind().equalsIgnoreCase("TOOL"))
                .sorted(Comparator.comparing(SpanRecord::sessionId)
                        .thenComparingInt(SpanRecord::turnSeq)
                        .thenComparing(SpanRecord::startedAt))
                .toList();

        Map<String, long[]> edges = new LinkedHashMap<>();
        Map<String, long[]> tools = new LinkedHashMap<>();
        String lastTool = null;
        String lastKey = null;
        for (SpanRecord span : toolSpans) {
            String tool = span.name();
            long[] totals = tools.computeIfAbsent(tool, k -> new long[2]);
            totals[0]++;
            if (span.status() != null && span.status().toUpperCase().contains("ERROR")) {
                totals[1]++;
            }
            String groupKey = span.sessionId() + "#" + span.turnSeq();
            if (lastTool != null && groupKey.equals(lastKey)) {
                String edgeKey = lastTool + "→" + tool;
                long[] counter = edges.computeIfAbsent(edgeKey, k -> new long[1]);
                counter[0]++;
            }
            lastTool = tool;
            lastKey = groupKey;
        }

        List<Edge> edgeList = new ArrayList<>();
        edges.forEach((key, counter) -> {
            int arrow = key.indexOf('→');
            edgeList.add(new Edge(key.substring(0, arrow), key.substring(arrow + 1), counter[0]));
        });
        edgeList.sort(Comparator.comparingLong(Edge::count).reversed()
                .thenComparing(Edge::from).thenComparing(Edge::to));

        List<ToolTotal> toolList = new ArrayList<>();
        tools.forEach((tool, totals) -> toolList.add(new ToolTotal(tool,
                totals[0], totals[1], totals[0] == 0 ? 0.0 : (double) totals[1] / totals[0])));
        toolList.sort(Comparator.comparingLong(ToolTotal::calls).reversed()
                .thenComparing(ToolTotal::tool));

        return new ToolGraphReport(List.copyOf(edgeList), List.copyOf(toolList));
    }
}
