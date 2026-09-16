package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 复制计数器（spec 2022 / T3145 / impl 1573）——CRDT G/PN-Counter 思想
 *（Dynamo/Cassandra 计数器口径）：per-writer 分量计数（同 writer 单调
 * 递推），merge 逐分量取 max（幂等/交换/结合——CRDT 三性质，重复
 * merge 与乱序 merge 同终态），全局值 = Σ分量；支持负增量即 PN 语义
 *（正计数 + 扣减共用一分量——writer 自律单调）。
 *
 * <p>synchronized 小临界区；确定性（TreeMap 快照字典序稳定）。
 */
public final class ReplicatedCounter {

    private final Map<String, Long> components = new HashMap<>();

    /** 记一次增量（正=计数、负=扣减；同 writer 分量单调由调用方自律——回退会被 merge 抹平）。 */
    public synchronized void increment(String writerId, long delta) {
        if (writerId == null) {
            throw new IllegalArgumentException("writerId 不能为 null");
        }
        components.merge(writerId, delta, Long::sum);
    }

    /** 全局值 = Σ per-writer 分量。 */
    public synchronized long value() {
        long sum = 0;
        for (long v : components.values()) {
            sum += v;
        }
        return sum;
    }

    /** per-writer 分量快照（字典序稳定——merge 载体与观测面）。 */
    public synchronized Map<String, Long> components() {
        return new TreeMap<>(components);
    }

    /**
     * 合并对端分量快照：逐 writer 取 max（因果保序——后见覆盖先见）。
     * 幂等：重复 merge 同快照不变；交换：A∪B = B∪A。
     */
    public synchronized void merge(Map<String, Long> other) {
        if (other == null) {
            throw new IllegalArgumentException("other 不能为 null");
        }
        for (Map.Entry<String, Long> e : other.entrySet()) {
            components.merge(e.getKey(), e.getValue(), Math::max);
        }
    }

    /** 合并对端计数器整件（快照口径便捷通道）。 */
    public synchronized void merge(ReplicatedCounter other) {
        if (other == null) {
            throw new IllegalArgumentException("other 不能为 null");
        }
        merge(other.components());
    }
}
