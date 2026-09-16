package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 老化优先级队列（spec 2024 / T3149 / impl 1575）——OS 调度 aging 思想：
 * 静态优先级的已知病是饥饿（高优先级洪峰下低优先级永不出队）——本件
 * 让等待本身生息：有效优先级 = 基础优先级 + 等待时长 × 老化速率，等得
 * 够久的低优先级终将反超新来的高优先级（反饥饿下界保证）；同有效分
 * FIFO 保序。
 *
 * <p>poll 线性扫描（O(n)——队列为准入级小规模，诚实边界入档）；时间
 * 由调用方传入（确定性可回放）；synchronized 小临界区。
 */
public final class AgingPriorityQueue<T> {

    /** 入队项：元素 + 基础优先级 + 入队时刻。 */
    private record Queued<T>(T item, int basePriority, long enqueueAtMillis, long seq) {
    }

    /** 默认老化速率（优先级点/秒）——1 点/秒：百级基础差百秒内反超。 */
    public static final double DEFAULT_AGING_RATE_PER_SECOND = 1.0d;

    private final double agingRatePerSecond;
    private final Deque<Queued<T>> entries = new ArrayDeque<>();
    private long seqCounter;

    /** 契约：agingRatePerSecond ≥ 0（0 = 退化静态优先级；fail-fast）。 */
    public AgingPriorityQueue(double agingRatePerSecond) {
        if (!(agingRatePerSecond >= 0) || Double.isNaN(agingRatePerSecond)) {
            throw new IllegalArgumentException("agingRate 须 ≥ 0：" + agingRatePerSecond);
        }
        this.agingRatePerSecond = agingRatePerSecond;
    }

    public AgingPriorityQueue() {
        this(DEFAULT_AGING_RATE_PER_SECOND);
    }

    /** 入队（基础优先级高者优先的底座；同刻同分按入队序）。契约：item 非空、base ∈ [0,100]、now ≥ 0。 */
    public synchronized void enqueue(T item, int basePriority, long nowMillis) {
        if (item == null) {
            throw new IllegalArgumentException("item 不能为 null");
        }
        if (basePriority < 0 || basePriority > 100) {
            throw new IllegalArgumentException("basePriority 须在 [0,100]：" + basePriority);
        }
        if (nowMillis < 0) {
            throw new IllegalArgumentException("nowMillis 须 ≥ 0：" + nowMillis);
        }
        entries.addLast(new Queued<>(item, basePriority, nowMillis, seqCounter++));
    }

    /** 有效优先级 = base + 等待秒 × 老化速率（观测面/测试面）。 */
    public synchronized double effectivePriority(T item, int basePriority,
                                                 long enqueueAt, long nowMillis) {
        return basePriority + (nowMillis - enqueueAt) / 1000.0d * agingRatePerSecond;
    }

    /** 出队：有效优先级最大者；同分入队序先出。空队返 null。 */
    public synchronized T poll(long nowMillis) {
        if (nowMillis < 0) {
            throw new IllegalArgumentException("nowMillis 须 ≥ 0：" + nowMillis);
        }
        if (entries.isEmpty()) {
            return null;
        }
        Queued<T> best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Queued<T> q : entries) {
            double score = effectivePriority(q.item(), q.basePriority(),
                    q.enqueueAtMillis(), nowMillis);
            if (score > bestScore || (score == bestScore && best != null
                    && q.seq() < best.seq())) {
                best = q;
                bestScore = score;
            }
        }
        entries.remove(best);
        return best.item();
    }

    /** 队列深度（积压水位）。 */
    public synchronized int size() {
        return entries.size();
    }

    /** 快照只读面（有效优先级排序，不出队——观测用）。 */
    public synchronized List<T> snapshotByEffectivePriority(long nowMillis) {
        if (nowMillis < 0) {
            throw new IllegalArgumentException("nowMillis 须 ≥ 0：" + nowMillis);
        }
        List<Queued<T>> copy = new ArrayList<>(entries);
        copy.sort((a, b) -> {
            double sa = effectivePriority(a.item(), a.basePriority(), a.enqueueAtMillis(), nowMillis);
            double sb = effectivePriority(b.item(), b.basePriority(), b.enqueueAtMillis(), nowMillis);
            int cmp = Double.compare(sb, sa); // 高分在前
            return cmp != 0 ? cmp : Long.compare(a.seq(), b.seq());
        });
        return copy.stream().map(Queued::item).toList();
    }
}
