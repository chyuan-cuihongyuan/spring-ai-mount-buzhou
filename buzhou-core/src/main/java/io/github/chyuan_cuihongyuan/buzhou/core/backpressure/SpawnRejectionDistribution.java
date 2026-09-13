package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话准入拒绝分布（spec 831 / T1163，k8s admission 拒绝读数扩散；
 * SpawnGate 拒绝事件的聚合面）：拒绝原因（admission-floor/fail-fast/
 * timeout/drain…开集字符串）各计多少、何时最近——「会话为什么进不来」
 * 从事件流考古变聚合表。
 *
 * <p>纯记账：原因开集 → 键封顶 {@value #MAX_REASONS}（超限不再记、
 * truncated 如实）；synchronized 计数+lastSeen；snapshot 计数降序+dominant。
 * 喂点=拒绝事件消费者装配侧（不改 SpawnGate 行为）。
 */
public final class SpawnRejectionDistribution {

    /** 单原因行。 */
    public record ReasonCount(String reason, long count, long lastSeenMillis) {
    }

    /** 不可变报告（counts 计数降序）。 */
    public record Report(List<ReasonCount> counts, long total, String dominant, boolean truncated) {
    }

    /** 原因键封顶。 */
    public static final int MAX_REASONS = 16;

    private final Map<String, long[]> counters = new LinkedHashMap<>(); // count, lastSeen
    private long total;
    private volatile boolean truncated;

    /** 记录一次拒绝（null/空白原因忽略）。 */
    public synchronized void record(String reason, long atMillis) {
        if (reason == null || reason.isBlank()) {
            return;
        }
        long[] agg = counters.get(reason);
        if (agg == null) {
            if (counters.size() >= MAX_REASONS) {
                truncated = true;
                return;
            }
            agg = new long[2];
            counters.put(reason, agg);
        }
        agg[0]++;
        agg[1] = Math.max(agg[1], atMillis);
        total++;
    }

    /** 只读快照（计数降序；dominant=计数最高者，平局保持先达）。 */
    public synchronized Report snapshot() {
        List<ReasonCount> rows = new ArrayList<>(counters.size());
        long best = 0;
        String top = null;
        for (Map.Entry<String, long[]> e : counters.entrySet()) {
            rows.add(new ReasonCount(e.getKey(), e.getValue()[0], e.getValue()[1]));
            if (e.getValue()[0] > best) {
                best = e.getValue()[0];
                top = e.getKey();
            }
        }
        rows.sort(Comparator.comparingLong(ReasonCount::count).reversed());
        return new Report(List.copyOf(rows), total, top, truncated);
    }
}
