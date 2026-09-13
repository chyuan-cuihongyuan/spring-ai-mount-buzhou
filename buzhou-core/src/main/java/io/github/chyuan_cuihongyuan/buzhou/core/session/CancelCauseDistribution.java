package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 取消原因分布读数（spec 824 / T1149，Temporal cancellation 语义观测借鉴；
 * 606 CancelCause 闭集的分布面）：五类取消原因（用户/停机排水/预算到期/
 * 租约丢失/失控终止）各计多少、谁主导、最近一次何时——「会话为什么死」
 * 从异常考古变一眼分布。
 *
 * <p>纯记账（闭集枚举——天然有界无封顶问题）：record(cause) 计数+lastSeen；
 * snapshot 按 counts 降序出报告（cause/count/share/lastSeen）。喂点归取消
 * 路径装配侧（不改 cancel 行为）。
 */
public final class CancelCauseDistribution {

    /** 单原因读数行。 */
    public record CauseCount(CancelCause cause, long count, double share, long lastSeenMillis) {
    }

    /** 不可变报告（counts 降序；dominant=计数最高者）。 */
    public record Report(List<CauseCount> counts, long total, CancelCause dominant) {
    }

    private final Map<CancelCause, long[]> counters = new EnumMap<>(CancelCause.class);
    private final Map<CancelCause, Long> lastSeen = new EnumMap<>(CancelCause.class);
    private long total;

    /** 记录一次取消（null 忽略）。 */
    public synchronized void record(CancelCause cause, long atMillis) {
        if (cause == null) {
            return;
        }
        counters.computeIfAbsent(cause, c -> new long[1])[0]++;
        lastSeen.merge(cause, atMillis, Math::max);
        total++;
    }

    /** 只读快照（counts 降序；无记录空报告）。 */
    public synchronized Report snapshot() {
        List<CauseCount> rows = new java.util.ArrayList<>(counters.size());
        CancelCause dominant = null;
        long best = 0;
        for (Map.Entry<CancelCause, long[]> e : counters.entrySet()) {
            long count = e.getValue()[0];
            rows.add(new CauseCount(e.getKey(), count,
                    total == 0 ? 0 : (double) count / total,
                    lastSeen.getOrDefault(e.getKey(), Long.MIN_VALUE)));
            if (count > best) {
                best = count;
                dominant = e.getKey();
            }
        }
        rows.sort(java.util.Comparator.comparingLong(CauseCount::count).reversed());
        return new Report(List.copyOf(rows), total, dominant);
    }
}
