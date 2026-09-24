package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CRDT PN-Counter（spec 5043 / T6187 / impl 2194）——
 * Riak/Redis CRDT 正负计数器思想：P（增）与 N（减）两个
 * G-Counter 组成，G-Counter=每节点单调计数表——副本各自
 * 本地增减，`merge` 按节点取 **max**（交换律/幂等律/结合律
 * 三律成立——任意顺序同步副本必收敛，无协调无丢失）——
 * 分布式计数靠中心序列化（协调开销+可用性损失）与
 * last-write-wins 覆盖（并发增量互相吞）的病解。
 * 确定性无时间依赖（无墙钟、无顺序假设）。
 *
 * <p>与 VectorClockOrder（concurrent）同族不同面：因果序
 * 判定 vs 收敛计数；与 ReadRepair（spec 5018）互补：
 * 读路径修复 vs 无冲突可合并状态。
 */
public final class CrdtPnCounter {

    private final Map<String, Long> increments = new LinkedHashMap<>();

    private final Map<String, Long> decrements = new LinkedHashMap<>();

    /** 本副本某节点递增（节点计数单调加；amount≥0）。 */
    public void increment(String nodeId, long amount) {
        requireNode(nodeId);
        requireAmount(amount);
        increments.merge(nodeId, amount, Long::sum);
    }

    /** 本副本某节点递减（N 侧单调加；amount≥0；总值可负）。 */
    public void decrement(String nodeId, long amount) {
        requireNode(nodeId);
        requireAmount(amount);
        decrements.merge(nodeId, amount, Long::sum);
    }

    /** 当前总值（P 侧和 − N 侧和）。 */
    public long value() {
        return sumOf(increments) - sumOf(decrements);
    }

    /**
     * 合并他副本状态（按节点取 max——交换/幂等/结合三律；
     * 原地合并返回自身便于链式）。
     */
    public CrdtPnCounter merge(CrdtPnCounter other) {
        if (other == null) {
            throw new IllegalArgumentException("other 非空");
        }
        mergeSide(increments, other.increments);
        mergeSide(decrements, other.decrements);
        return this;
    }

    /** P 侧单调表读数（副本审计面——不可变副本）。 */
    public Map<String, Long> incrementEntries() {
        return Map.copyOf(increments);
    }

    /** N 侧单调表读数（副本审计面——不可变副本）。 */
    public Map<String, Long> decrementEntries() {
        return Map.copyOf(decrements);
    }

    private static void mergeSide(Map<String, Long> mine, Map<String, Long> theirs) {
        for (Map.Entry<String, Long> entry : theirs.entrySet()) {
            mine.merge(entry.getKey(), entry.getValue(), Math::max);
        }
    }

    private static long sumOf(Map<String, Long> side) {
        long total = 0;
        for (Long value : side.values()) {
            total += value;
        }
        return total;
    }

    private static void requireNode(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            throw new IllegalArgumentException("nodeId 非空");
        }
    }

    private static void requireAmount(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount≥0：" + amount);
        }
    }
}
