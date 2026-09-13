package io.github.chyuan_cuihongyuan.buzhou.observability;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 尾采样决策台账（spec 835 / T1171，OpenTelemetry Collector tail_sampling
 * processor 借鉴）：trace 级采样决策（KEPT/DROPPED+策略原因）环形留痕+按
 * 决策与原因聚合+保留率——「trace 为什么没了」从猜变查（与 TurnErrorSampler
 * 的 eval 采样域正交）。
 *
 * <p>纯记账：决策二值闭集+原因开集（键封顶 {@value #MAX_REASONS}，超限并入
 * overflow 桶——跨域口径一致）；环形明细 {@value #RING_CAPACITY}（挤最老计
 * dropped）。喂点=采样器装配侧。
 */
public final class TailSamplingDecisionLog {

    /** 环形明细容量。 */
    public static final int RING_CAPACITY = 64;
    /** 原因键封顶。 */
    public static final int MAX_REASONS = 16;
    /** 溢出桶名。 */
    public static final String OVERFLOW = "__overflow__";

    /** 决策二值。 */
    public enum Decision { KEPT, DROPPED }

    /** 单条决策明细。 */
    public record Entry(String traceId, Decision decision, String reason, long atMillis) {
    }

    /** 按原因聚合行。 */
    public record ReasonCount(Decision decision, String reason, long count) {
    }

    /** 不可变报告。 */
    public record Report(List<Entry> recent, List<ReasonCount> byReason, long kept, long droppedCount,
                         double keptRatio, long ringDropped, boolean truncated) {
    }

    private final Deque<Entry> ring = new ArrayDeque<>(RING_CAPACITY);
    private final Map<String, long[]> byReason = new LinkedHashMap<>(); // 决策ordinal+reason → count
    private final Object lock = new Object();
    private long kept;
    private long droppedCount;
    private long ringDropped;
    private boolean truncated;

    /** 记录一次采样决策（null/空白 traceId/reason 忽略）。 */
    public void record(String traceId, Decision decision, String reason, long atMillis) {
        if (traceId == null || traceId.isBlank() || decision == null
                || reason == null || reason.isBlank()) {
            return;
        }
        synchronized (lock) {
            if (ring.size() >= RING_CAPACITY) {
                ring.pollFirst();
                ringDropped++;
            }
            ring.addLast(new Entry(traceId, decision, reason, atMillis));
            String key = decision.name() + "\u0000" + reason;
            long[] agg = byReason.get(key);
            if (agg == null) {
                if (byReason.size() >= MAX_REASONS) {
                    truncated = true;
                    key = decision.name() + "\u0000" + OVERFLOW;
                    agg = byReason.computeIfAbsent(key, k -> new long[1]);
                } else {
                    agg = byReason.computeIfAbsent(key, k -> new long[1]);
                }
            }
            agg[0]++;
            if (decision == Decision.KEPT) {
                kept++;
            } else {
                droppedCount++;
            }
        }
    }

    /** 只读快照（recent 新→旧；byReason 计数降序）。 */
    public Report snapshot() {
        synchronized (lock) {
            List<Entry> recent = new ArrayList<>(ring);
            java.util.Collections.reverse(recent);
            List<ReasonCount> rows = new ArrayList<>();
            for (Map.Entry<String, long[]> e : byReason.entrySet()) {
                String[] parts = e.getKey().split("\u0000", 2);
                rows.add(new ReasonCount(Decision.valueOf(parts[0]), parts[1], e.getValue()[0]));
            }
            rows.sort(java.util.Comparator.comparingLong(ReasonCount::count).reversed());
            long total = kept + droppedCount;
            return new Report(List.copyOf(recent), List.copyOf(rows), kept, droppedCount,
                    total == 0 ? 0 : (double) kept / total, ringDropped, truncated);
        }
    }
}
