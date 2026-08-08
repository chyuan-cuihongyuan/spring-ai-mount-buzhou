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

    public DashboardQueryService(ObservabilityStore store) {
        this.store = store;
    }

    // ---- DTO ----

    /** 会话分页：nextCursor 为 null 表示没有下一页。 */
    public record SessionPage(List<SessionSummary> items, String nextCursor) {}

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
