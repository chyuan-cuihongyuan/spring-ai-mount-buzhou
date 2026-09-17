package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 虚拟运行时间公平队列（spec 3031 / T5063 / impl 2032）——Linux
 * CFS vruntime 思想：每实体记**虚拟运行时间**（实际开销按权重折
 * 算 vruntime += work/weight——重权者走得慢），恒取 vruntime 最小
 * 者调度——长期看各实体按权重比例分得吞吐（等权即轮转、三倍权
 * 三倍配额），免时间片轮转的静态分片僵化。「工具配额/租户公平/
 * 会话资源份额」的加权公平原语。
 *
 * <p>TreeMap O(log n) 取最小（CFS 红黑树同构）；并列取先入序
 * （确定性）；work=0 纯重选不前进；非线程安全。
 */
public final class VirtualRuntimeQueue {

    /** 排序键（vruntime 先比，并列先入先选——reinsert 计数器保确定性）。 */
    private record SortKey(double vruntime, long insertion) implements Comparable<SortKey> {
        @Override
        public int compareTo(SortKey other) {
            int byVruntime = Double.compare(vruntime, other.vruntime);
            return byVruntime != 0 ? byVruntime : Long.compare(insertion, other.insertion);
        }
    }

    private static final double MIN_WEIGHT = 1.0;

    private final Map<String, Double> weights = new HashMap<>();
    private final Map<String, Double> vruntimes = new HashMap<>();
    private final TreeMap<SortKey, String> ordered = new TreeMap<>();
    private long insertionCounter;

    /** 注册实体（id 唯一；weight ≥ 1——越大配额越多）。 */
    public void register(String id, double weight) {
        if (id == null || weights.containsKey(id)) {
            throw new IllegalArgumentException("id 唯一非空：" + id);
        }
        if (!(weight >= MIN_WEIGHT)) {
            throw new IllegalArgumentException("weight ≥ 1：" + weight);
        }
        weights.put(id, weight);
        vruntimes.put(id, 0.0);
        ordered.put(new SortKey(0.0, insertionCounter++), id);
    }

    /**
     * 取 vruntime 最小者并推进其账面（vruntime += work/weight——
     * 重权者慢走）；返回 id，空队列 null。work=0 纯重选不前进。
     */
    public String pickNext(double workUnits) {
        if (!(workUnits >= 0)) {
            throw new IllegalArgumentException("work ≥ 0：" + workUnits);
        }
        Map.Entry<SortKey, String> head = ordered.firstEntry();
        if (head == null) {
            return null;
        }
        ordered.remove(head.getKey());
        String id = head.getValue();
        double advanced = head.getKey().vruntime() + workUnits / weights.get(id);
        vruntimes.put(id, advanced);
        ordered.put(new SortKey(advanced, insertionCounter++), id);
        return id;
    }

    /** 实体虚拟运行时间（未注册 NaN）。 */
    public double vruntimeOf(String id) {
        return vruntimes.getOrDefault(id, Double.NaN);
    }

    /** 实体在册判定。 */
    public boolean contains(String id) {
        return weights.containsKey(id);
    }

    /** 在册实体数。 */
    public int size() {
        return weights.size();
    }
}
