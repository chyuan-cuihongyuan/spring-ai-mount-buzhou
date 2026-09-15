package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import java.util.ArrayList;
import java.util.List;

/**
 * 回放时钟偏斜读面（L 会话 1700 系 R23 = effort #1722 / spec 1722 /
 * 票 T2645 + T2646 / impl 1322）——Kafka consumer lag / NTP 偏斜思想：
 * 回放（重放事件流重建状态）时「回放时刻 − 事件原时刻」的偏斜分布回答
 * 「回放落后多远、有没有时钟倒挂」——负偏斜 = 倒挂（时钟不可信）。
 *
 * <p>纯函数零状态：`analyze(List<long[]>{originalAt, replayedAt})` 也可用
 * 实例面累积。定实例面（与 RepairOutcomeStats 同域）：`record(originalAt,
 * replayedAt)` 逐笔累积 +`report()` 中位/最大/负偏斜计数。负偏斜计入
 * negativeCount 且偏斜值按 0 参与正偏斜统计（诚实分离倒挂与滞后）。
 *
 * @since 1.0.0
 */
public final class ReplaySkewStats {

    private final List<Long> positiveSkews = new ArrayList<>();
    private long negativeCount;
    private long total;

    /** 记一笔回放（replayedAt &lt; originalAt = 时钟倒挂，计入负偏斜）。 */
    public synchronized void record(long originalAtMillis, long replayedAtMillis) {
        total++;
        long skew = replayedAtMillis - originalAtMillis;
        if (skew < 0) {
            negativeCount++;
        } else {
            positiveSkews.add(skew);
        }
    }

    /**
     * @param samples        样本总数
     * @param medianLagMillis 正偏斜中位（无正样本哨兵 −1）
     * @param maxLagMillis   最大正偏斜（无正样本哨兵 −1）
     * @param negativeSkewCount 时钟倒挂笔数（replayedAt&lt;originalAt）
     */
    public record SkewReport(long samples, long medianLagMillis,
                             long maxLagMillis, long negativeSkewCount) {
    }

    /** 快照。 */
    public synchronized SkewReport report() {
        if (positiveSkews.isEmpty()) {
            return new SkewReport(total, -1L, -1L, negativeCount);
        }
        List<Long> sorted = new ArrayList<>(positiveSkews);
        sorted.sort(Long::compare);
        int n = sorted.size();
        long median = n % 2 == 1
                ? sorted.get(n / 2)
                : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2;
        return new SkewReport(total, median, sorted.get(n - 1), negativeCount);
    }
}
