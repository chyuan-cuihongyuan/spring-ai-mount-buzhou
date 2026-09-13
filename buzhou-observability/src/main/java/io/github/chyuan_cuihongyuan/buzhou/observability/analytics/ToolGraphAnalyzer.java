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

    /** 环枚举封顶（基数有界纪律——超限停止搜索，枚举顺序确定→截断确定）。 */
    public static final int MAX_CYCLES = 16;

    /**
     * 初等环枚举（spec 717 / T985，静态分析 call-graph 环检测借鉴）：报告的有向
     * 边集合上 DFS——<b>锚去重</b>（每环以最小节点为锚，锚外不扩展——A→B→A 与
     * B→A→B 只报一次）；路径 visited 防自交；自环（A→A）单独识别。输出按
     * （长度, 字典序）稳定排序；超 {@value #MAX_CYCLES} 停止。空图/无边图 = 空表。
     */
    public static List<List<String>> cycles(ToolGraphReport report) {
        if (report == null) {
            throw new IllegalArgumentException("report 必须非空");
        }
        Map<String, java.util.Set<String>> adjacency = new LinkedHashMap<>();
        java.util.TreeSet<String> nodes = new java.util.TreeSet<>();
        for (Edge edge : report.edges()) {
            if (edge.count() <= 0) {
                continue;
            }
            nodes.add(edge.from());
            nodes.add(edge.to());
            adjacency.computeIfAbsent(edge.from(), k -> new java.util.TreeSet<>()).add(edge.to());
        }
        List<List<String>> cycles = new ArrayList<>();
        for (String anchor : nodes) {
            dfsCycles(anchor, anchor, adjacency, new ArrayList<>(List.of(anchor)),
                    new java.util.LinkedHashSet<>(List.of(anchor)), cycles);
            if (cycles.size() >= MAX_CYCLES) {
                break;
            }
        }
        cycles.sort(Comparator.<List<String>>comparingInt(List::size)
                .thenComparing(list -> String.join("→", list)));
        if (cycles.size() > MAX_CYCLES) {
            return List.copyOf(cycles.subList(0, MAX_CYCLES));
        }
        return List.copyOf(cycles);
    }

    /** 锚定 DFS：只记录回到锚的环；中间节点必须 > 锚（最小节点锚定——旋转去重）。 */
    private static void dfsCycles(String anchor, String current,
            Map<String, java.util.Set<String>> adjacency,
            List<String> path, java.util.LinkedHashSet<String> inPath,
            List<List<String>> out) {
        for (String next : adjacency.getOrDefault(current, java.util.Set.of())) {
            if (out.size() >= MAX_CYCLES) {
                return;
            }
            if (next.equals(anchor)) {
                out.add(List.copyOf(path)); // 找到环（长度 ≥2；自环即长度 1）
                continue;
            }
            if (inPath.contains(next) || next.compareTo(anchor) < 0) {
                continue; // 自交 / 非锚最小节点（旋转去重）
            }
            path.add(next);
            inPath.add(next);
            dfsCycles(anchor, next, adjacency, path, inPath, out);
            path.remove(path.size() - 1);
            inPath.remove(next);
        }
    }

    /**
     * impl-659 / spec 906：per-tool 耗时画像（flamegraph self/cumulative 思想）——
     * 「哪个工具自身最耗时」（self）vs「哪条 agent-as-tool 调用链最贵」（cumulative）。
     *
     * <p>TOOL span 过滤口径同 {@link #analyze(List)}（kind 忽略大小写）；层级按
     * {@code parentSpanId} 在 TOOL 子集内解析（父不在集合=根；parent 指针环经
     * visiting 防护——数据损坏断开记 0 不死循环）；span 耗时 = {@code endedAt −
     * startedAt}（RUNNING 中间态/null 端点计 0——诚实不估；负值时钟偏移夹 0）。
     *
     * <p>输出按 {@code totalSelfMs} 降序 + tool 字典序 tie-break（稳定确定——
     * 火焰图宽板块在前直觉）。与 core 的 ToolTimingAggregator（spec 700 进程内
     * 热路径）互补：本面为离线全量 span 历史归因。
     */
    public record ToolTimingProfile(String tool, long calls, long totalSelfMs,
                                    long totalCumulativeMs) {
    }

    public static List<ToolTimingProfile> timings(List<SpanRecord> spans) {
        if (spans == null) {
            throw new IllegalArgumentException("spans 必须非空");
        }
        List<SpanRecord> toolSpans = spans.stream()
                .filter(s -> s.kind() != null && s.kind().equalsIgnoreCase("TOOL"))
                .toList();

        Map<String, SpanRecord> byId = new LinkedHashMap<>();
        for (SpanRecord span : toolSpans) {
            if (span.spanId() != null) {
                byId.put(span.spanId(), span);
            }
        }
        Map<String, List<SpanRecord>> childrenOf = new LinkedHashMap<>();
        for (SpanRecord span : toolSpans) {
            SpanRecord parent = span.parentSpanId() == null ? null : byId.get(span.parentSpanId());
            if (parent == null || parent == span) {
                continue; // 根（父不在 TOOL 集 / 自环）——无入边
            }
            childrenOf.computeIfAbsent(parent.spanId(), k -> new ArrayList<>()).add(span);
        }

        Map<String, Long> cumulativeMemo = new LinkedHashMap<>();
        java.util.Set<String> visiting = new java.util.HashSet<>();
        Map<String, long[]> byTool = new LinkedHashMap<>(); // [calls, selfMs, cumulativeMs]
        for (SpanRecord span : toolSpans) {
            long self = durationMs(span);
            long cumulative = cumulativeMs(span, childrenOf, cumulativeMemo, visiting);
            long[] totals = byTool.computeIfAbsent(span.name(), k -> new long[3]);
            totals[0]++;
            totals[1] += self;
            totals[2] += cumulative;
        }

        List<ToolTimingProfile> profiles = new ArrayList<>();
        byTool.forEach((tool, totals) -> profiles.add(
                new ToolTimingProfile(tool, totals[0], totals[1], totals[2])));
        profiles.sort(Comparator.comparingLong(ToolTimingProfile::totalSelfMs).reversed()
                .thenComparing(ToolTimingProfile::tool));
        return List.copyOf(profiles);
    }

    /** span 耗时（毫秒；null 端点/RUNNING 计 0；负值时钟偏移夹 0）。 */
    private static long durationMs(SpanRecord span) {
        if (span.startedAt() == null || span.endedAt() == null) {
            return 0;
        }
        long millis = java.time.Duration.between(span.startedAt(), span.endedAt()).toMillis();
        return Math.max(0, millis);
    }

    /** 子树累计耗时（含自身；环断开记 0——数据损坏诚实容忍）。 */
    private static long cumulativeMs(SpanRecord span, Map<String, List<SpanRecord>> childrenOf,
            Map<String, Long> memo, java.util.Set<String> visiting) {
        if (span.spanId() != null) {
            Long cached = memo.get(span.spanId());
            if (cached != null) {
                return cached;
            }
            if (!visiting.add(span.spanId())) {
                return 0; // parent 指针环——断开
            }
        }
        long total = durationMs(span);
        for (SpanRecord child : childrenOf.getOrDefault(span.spanId(), List.of())) {
            total += cumulativeMs(child, childrenOf, memo, visiting);
        }
        if (span.spanId() != null) {
            visiting.remove(span.spanId());
            memo.put(span.spanId(), total);
        }
        return total;
    }
}
