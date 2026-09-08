package io.github.chyuan_cuihongyuan.buzhou.dashboard;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InjectionSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dashboard 查询门面（spec 03 Dashboard 查询 API 的领域层；ticket 17）。
 *
 * <p>纯 Java 无 Web 依赖：HTTP 层（{@code DashboardHttpServer} 独立端口首发 /
 * ticket 20 的 MVC 控制器）薄包本类。统计口径 = Span 属性袋聚合
 * （验收：token/耗时统计与 Span 属性一致），不经第二份数据。
 */
public class DashboardQueryService {

    private final ObservabilityStore store;

    private final io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore indexStore;

    public DashboardQueryService(ObservabilityStore store) {
        this(store, null);
    }

    /** spec 36 §B / T122 / impl-97：索引优先装配（null = 观测留痕回退，既有行为）。 */
    public DashboardQueryService(ObservabilityStore store,
            io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore indexStore) {
        this.store = store;
        this.indexStore = indexStore;
    }

    // ---- DTO ----

    /** 会话分页：nextCursor 为 null 表示没有下一页。 */
    public record SessionPage(List<SessionSummary> items, String nextCursor) {}

    /** 过滤会话分页行（spec 36 §B：索引源——带状态/标签/app 维度）。 */
    public record IndexedSessionPage(
            List<io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo> items,
            String nextCursor, boolean fromIndex) {}

    /** Span 树节点（view=tree 服务端组树）。 */
    public record SpanNode(SpanRecord span, List<SpanNode> children) {}

    /** 单轮回放：本轮 span 集合 + Event 流 + 是否有注入快照。 */
    public record TurnReplay(int turnSeq, SpanRecord turnSpan, List<SpanRecord> spans,
                             List<EventRecord> events, boolean hasSnapshot) {}

    /** 会话回放：轮次序列 + 每轮 Event 流；unboundEvents 为 span 归属缺失的孤儿事件。 */
    public record ReplayView(String sessionId, List<TurnReplay> turns,
                             List<EventRecord> unboundEvents) {}

    public record TurnStats(int turnSeq, long promptTokens, long completionTokens,
                            long durationMs, int iterations) {}

    public record ModelStats(String model, int calls, long promptTokens, long completionTokens,
                             long reasoningTokens, long totalDurationMs) {}

    public record ToolStats(String tool, int calls, int errors, long totalDurationMs) {}

    /** 会话统计：总量 + 按轮次 / 按模型 / 按工具分组（与 Span 属性口径一致）。 */
    public record SessionStats(String sessionId, long totalPromptTokens, long totalCompletionTokens,
                               long totalDurationMs, List<TurnStats> perTurn,
                               List<ModelStats> perModel, List<ToolStats> perTool) {}

    // ---- 查询 ----

    public SessionPage listSessions(String cursor, int size) {
        int offset = cursor == null || cursor.isBlank() ? 0 : Integer.parseInt(cursor);
        // 多取一条探测是否还有下一页，避免末页恰好 size 条时发出指向空页的 nextCursor
        List<SessionSummary> probed = store.listSessionSummaries(cursor, size + 1);
        boolean hasMore = probed.size() > size;
        List<SessionSummary> items = hasMore ? probed.subList(0, size) : probed;
        return new SessionPage(List.copyOf(items),
                hasMore ? String.valueOf(offset + size) : null);
    }

    /**
     * 过滤会话列表（spec 36 §B / T122 / impl-97）：SessionIndexStore 装配时走索引
     * （appId/agentName/status/tag 过滤 + lastActive 倒序；DELETED 默认排除）；未装配
     * 回退观测留痕（无过滤维度，参数被忽略并标记 fromIndex=false——诚实降级）。
     */
    public IndexedSessionPage listSessionsFiltered(String appId, String agentName, String status,
            String tagKey, String tagValue, String cursor, int size) {
        int offset = cursor == null || cursor.isBlank() ? 0 : Integer.parseInt(cursor);
        int pageSize = Math.max(1, Math.min(size, 200));
        if (indexStore != null) {
            List<io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo> probed =
                    indexStore.list(new io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery(
                            appId, agentName, status, tagKey, tagValue, offset, pageSize + 1));
            boolean hasMore = probed.size() > pageSize;
            List<io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo> items =
                    hasMore ? probed.subList(0, pageSize) : probed;
            return new IndexedSessionPage(List.copyOf(items),
                    hasMore ? String.valueOf(offset + pageSize) : null, true);
        }
        // 回退：观测留痕（无过滤维度——调用方以 fromIndex=false 感知降级）
        SessionPage fallback = listSessions(cursor, pageSize);
        List<io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo> items =
                fallback.items().stream()
                        .map(sum -> new io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo(
                                sum.sessionId(), null, null,
                                io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo.STATUS_ACTIVE,
                                sum.firstActivityAt() == null ? 0L
                                        : sum.firstActivityAt().toEpochMilli(),
                                sum.lastActivityAt() == null ? 0L
                                        : sum.lastActivityAt().toEpochMilli(),
                                sum.turnCount(), java.util.Map.of()))
                        .toList();
        return new IndexedSessionPage(items, fallback.nextCursor(), false);
    }

    /** 会话回放：轮次序列 + 每轮 Event 流（Thinking/FinalReply/工具出入参）。 */
    public ReplayView replay(String sessionId) {
        List<SpanRecord> spans = store.spansOfSession(sessionId);
        List<EventRecord> events = store.eventsOfSession(sessionId);
        Map<String, SpanRecord> spanById = new LinkedHashMap<>();
        spans.forEach(s -> spanById.put(s.spanId(), s));

        Map<Integer, List<SpanRecord>> spansByTurn = new LinkedHashMap<>();
        for (SpanRecord s : spans) {
            if (s.turnSeq() >= 0) {
                spansByTurn.computeIfAbsent(s.turnSeq(), k -> new ArrayList<>()).add(s);
            }
        }
        Map<Integer, List<EventRecord>> eventsByTurn = new LinkedHashMap<>();
        List<EventRecord> unbound = new ArrayList<>();
        for (EventRecord e : events) {
            SpanRecord owner = spanById.get(e.spanId());
            if (owner == null || owner.turnSeq() < 0) {
                unbound.add(e);
            } else {
                eventsByTurn.computeIfAbsent(owner.turnSeq(), k -> new ArrayList<>()).add(e);
            }
        }
        List<TurnReplay> turns = new ArrayList<>();
        spansByTurn.forEach((turnSeq, turnSpans) -> {
            SpanRecord turnSpan = turnSpans.stream()
                    .filter(s -> SpanKind.TURN.equals(s.kind())).findFirst().orElse(null);
            turns.add(new TurnReplay(turnSeq, turnSpan, List.copyOf(turnSpans),
                    List.copyOf(eventsByTurn.getOrDefault(turnSeq, List.of())),
                    store.injectionSnapshot(sessionId, turnSeq).isPresent()));
        });
        turns.sort(Comparator.comparingInt(TurnReplay::turnSeq));
        return new ReplayView(sessionId, List.copyOf(turns), List.copyOf(unbound));
    }

    /** Span 拉取：view=flat 平铺（前端组树）；view=tree 服务端组树。 */
    public Object spans(String sessionId, String view) {
        List<SpanRecord> spans = store.spansOfSession(sessionId).stream()
                .sorted(Comparator.comparing(SpanRecord::startedAt)).toList();
        if (!"tree".equals(view)) {
            return spans;
        }
        Map<String, SpanNode> nodes = new LinkedHashMap<>();
        spans.forEach(s -> nodes.put(s.spanId(), new SpanNode(s, new ArrayList<>())));
        List<SpanNode> roots = new ArrayList<>();
        for (SpanNode node : nodes.values()) {
            SpanNode parent = node.span().parentSpanId() == null
                    ? null : nodes.get(node.span().parentSpanId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children().add(node);
            }
        }
        return roots;
    }

    public List<EventRecord> eventsOfSpan(String spanId) {
        return store.eventsOfSpan(spanId);
    }

    /** 注入快照：还原「模型当时实际看到什么」（消息序列 + 预算明细 + 策略版本）。 */
    public Optional<InjectionSnapshot> snapshot(String sessionId, int turnSeq) {
        return store.injectionSnapshot(sessionId, turnSeq);
    }

    /** token/耗时统计：按轮次、按模型、按工具分组，全部从 Span 属性袋聚合。 */
    /** 时间桶预聚合行（spec 412 / T715，M3 fixed-window downsampling 借鉴）。 */
    public record TimeBucket(Instant start, int turns, int modelCalls, int toolCalls,
            int errors, long promptTokens, long completionTokens) {
    }

    /** 桶数上界（payload 纪律）。 */
    public static final int MAX_ROLLUP_BUCKETS = 1000;

    /**
     * 时间桶预聚合（spec 412 / T715）：全会话翻页枚举 → TURN/MODEL_CALL/TOOL_CALL
     * 三类入窗 → epoch 对齐分桶 → 升序 + 空桶补齐（图表连续性——Prometheus rate
     * 需连续桶）；桶数超上界 IllegalArgumentException。只读查询零行为变化。
     */
    public List<TimeBucket> rollups(Instant from, Instant to, java.time.Duration bucket) {
        if (from == null || to == null || bucket == null || !to.isAfter(from)) {
            throw new IllegalArgumentException("from/to/bucket 非空且 to > from");
        }
        if (bucket.isZero() || bucket.isNegative()) {
            throw new IllegalArgumentException("bucket 为正时长");
        }
        long bucketMs = bucket.toMillis();
        long total = (to.toEpochMilli() - from.toEpochMilli() + bucketMs - 1) / bucketMs;
        if (total > MAX_ROLLUP_BUCKETS) {
            throw new IllegalArgumentException("桶数 " + total + " 超上界 " + MAX_ROLLUP_BUCKETS
                    + "——加粗桶粒度或缩窗");
        }
        Map<Long, long[]> byBucket = new java.util.TreeMap<>(); // [turns, modelCalls, toolCalls, errors, prompt, completion]
        for (long b = 0; b < total; b++) {
            byBucket.put(floorEpoch(from, bucketMs) + b * bucketMs, new long[6]);
        }
        String cursor = null;
        while (true) {
            SessionPage page = listSessions(cursor, 100);
            for (SessionSummary summary : page.items()) {
                for (SpanRecord s : store.spansOfSession(summary.sessionId())) {
                    collectRollup(s, from, to, bucketMs, byBucket);
                }
            }
            if (page.nextCursor() == null) {
                break;
            }
            cursor = page.nextCursor();
        }
        List<TimeBucket> out = new java.util.ArrayList<>(byBucket.size());
        byBucket.forEach((start, v) -> out.add(new TimeBucket(
                Instant.ofEpochMilli(start), (int) v[0], (int) v[1], (int) v[2],
                (int) v[3], v[4], v[5])));
        return List.copyOf(out);
    }

    private void collectRollup(SpanRecord s, Instant from, Instant to, long bucketMs,
            Map<Long, long[]> byBucket) {
        if (s.startedAt() == null || s.startedAt().isBefore(from) || !s.startedAt().isBefore(to)) {
            return;
        }
        boolean error = "ERROR".equals(s.status());
        switch (s.kind()) {
            case SpanKind.MODEL_CALL -> {
                long[] v = bucketOf(s.startedAt(), bucketMs, byBucket);
                if (v == null) {
                    return;
                }
                v[1]++;
                if (error) {
                    v[3]++;
                }
                v[4] += attrLong(s, "usage.prompt_tokens");
                v[5] += attrLong(s, "usage.completion_tokens");
            }
            case SpanKind.TOOL_CALL -> {
                long[] v = bucketOf(s.startedAt(), bucketMs, byBucket);
                if (v == null) {
                    return;
                }
                v[2]++;
                if (error) {
                    v[3]++;
                }
            }
            case SpanKind.TURN -> {
                long[] v = bucketOf(s.startedAt(), bucketMs, byBucket);
                if (v == null) {
                    return;
                }
                v[0]++;
                if (error) {
                    v[3]++;
                }
            }
            default -> { /* SESSION/HARNESS_INTERNAL 不进桶 */
            }
        }
    }

    private static long[] bucketOf(Instant at, long bucketMs, Map<Long, long[]> byBucket) {
        long key = floorEpoch(at, bucketMs);
        return byBucket.get(key);
    }

    /** epoch 对齐 floor（跨实例对齐口径一致）。 */
    private static long floorEpoch(Instant at, long bucketMs) {
        return Math.floorDiv(at.toEpochMilli(), bucketMs) * bucketMs;
    }

    public SessionStats stats(String sessionId) {
        List<SpanRecord> spans = store.spansOfSession(sessionId);

        Map<Integer, long[]> tokensByTurn = new LinkedHashMap<>();   // [prompt, completion]
        Map<Integer, Integer> iterationsByTurn = new LinkedHashMap<>();
        Map<String, ModelStatsBuilder> byModel = new LinkedHashMap<>();
        Map<String, ToolStatsBuilder> byTool = new LinkedHashMap<>();
        Instant first = null;
        Instant last = null;
        long totalPrompt = 0;
        long totalCompletion = 0;

        for (SpanRecord s : spans) {
            Instant activity = s.activityAt();
            if (s.startedAt() != null && (first == null || s.startedAt().isBefore(first))) {
                first = s.startedAt();
            }
            if (activity != null && (last == null || activity.isAfter(last))) {
                last = activity;
            }
            switch (s.kind()) {
                case SpanKind.MODEL_CALL -> {
                    long prompt = attrLong(s, "usage.prompt_tokens");
                    long completion = attrLong(s, "usage.completion_tokens");
                    long reasoning = attrLong(s, "usage.reasoning_tokens");
                    totalPrompt += prompt;
                    totalCompletion += completion;
                    tokensByTurn.computeIfAbsent(s.turnSeq(), k -> new long[2]);
                    tokensByTurn.get(s.turnSeq())[0] += prompt;
                    tokensByTurn.get(s.turnSeq())[1] += completion;
                    iterationsByTurn.merge(s.turnSeq(), 1, Integer::sum);
                    String model = attrString(s, "model.name", "unknown");
                    byModel.computeIfAbsent(model, k -> new ModelStatsBuilder())
                            .add(prompt, completion, reasoning, durationMs(s));
                }
                case SpanKind.TOOL_CALL -> {
                    String tool = attrString(s, "tool.name",
                            s.name() == null ? "unknown" : s.name().replaceFirst("^tool:", ""));
                    byTool.computeIfAbsent(tool, k -> new ToolStatsBuilder())
                            .add("ERROR".equals(s.status()), durationMs(s));
                }
                default -> { /* SESSION/TURN/HARNESS_INTERNAL 不进分组统计 */ }
            }
        }

        Map<Integer, Long> turnDurations = new LinkedHashMap<>();
        for (SpanRecord s : spans) {
            if (SpanKind.TURN.equals(s.kind())) {
                turnDurations.put(s.turnSeq(), durationMs(s));
            }
        }
        List<TurnStats> perTurn = new ArrayList<>();
        tokensByTurn.forEach((turnSeq, tokens) -> perTurn.add(new TurnStats(turnSeq,
                tokens[0], tokens[1], turnDurations.getOrDefault(turnSeq, 0L),
                iterationsByTurn.getOrDefault(turnSeq, 0))));
        perTurn.sort(Comparator.comparingInt(TurnStats::turnSeq));

        List<ModelStats> perModel = new ArrayList<>();
        byModel.forEach((model, b) -> perModel.add(b.build(model)));
        List<ToolStats> perTool = new ArrayList<>();
        byTool.forEach((tool, b) -> perTool.add(b.build(tool)));

        long totalDuration = first == null || last == null ? 0
                : Duration.between(first, last).toMillis();
        return new SessionStats(sessionId, totalPrompt, totalCompletion, totalDuration,
                List.copyOf(perTurn), List.copyOf(perModel), List.copyOf(perTool));
    }

    private static long durationMs(SpanRecord s) {
        if (s.startedAt() == null || s.endedAt() == null) {
            return 0;
        }
        return Duration.between(s.startedAt(), s.endedAt()).toMillis();
    }

    private static long attrLong(SpanRecord s, String key) {
        Object v = s.attributes().get(key);
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private static String attrString(SpanRecord s, String key, String fallback) {
        Object v = s.attributes().get(key);
        return v == null ? fallback : String.valueOf(v);
    }

    private static final class ModelStatsBuilder {
        private int calls;
        private long prompt;
        private long completion;
        private long reasoning;
        private long durationMs;

        void add(long p, long c, long r, long d) {
            calls++;
            prompt += p;
            completion += c;
            reasoning += r;
            durationMs += d;
        }

        ModelStats build(String model) {
            return new ModelStats(model, calls, prompt, completion, reasoning, durationMs);
        }
    }

    private static final class ToolStatsBuilder {
        private int calls;
        private int errors;
        private long durationMs;

        void add(boolean error, long d) {
            calls++;
            if (error) {
                errors++;
            }
            durationMs += d;
        }

        ToolStats build(String tool) {
            return new ToolStats(tool, calls, errors, durationMs);
        }
    }
}
