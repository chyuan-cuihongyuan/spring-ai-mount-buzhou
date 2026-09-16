package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * 指数直方图滑窗计数（spec 2002 / T3105 / impl 1553）——Datar-Indyk-
 * Lewkowitz-Rubinfeld 流式算法思想：滑动窗口计数不求全存——事件按 2 的
 * 幂分级入桶（同容量至多 2 桶，出现第 3 个即合并最老两个容量翻倍），
 * 整体过期桶惰性清出；估计 = 全界内桶计全 + 跨界桶计半（事件分布未知
 * 的无偏假设）——空间 O(log N) 桶数而非 O(N) 事件数，误差界 =
 * Σ跨界桶 ceil(capacity/2) 自描述（每跨界桶偏差至多计半份额）。
 *
 * <p>桶序按 last 非降维护（合并桶 set 回最老原位——时间序不破坏）；
 * 离散 tick 语义（insert 记事件于当前 tick、tick 推进时间一步）；
 * synchronized 小临界区；确定性（无随机数——同序列同答案可回放）。
 */
public final class ExponentialWindowCounter {

    /** 桶：容量（2 的幂）、首末事件 tick（跨度判定跨界的两端口径）。 */
    private record Bucket(long capacity, long firstTimestamp, long lastTimestamp) {
    }

    private final long window;
    private final List<Bucket> buckets = new ArrayList<>(); // 老 → 新（last 非降序）
    private long now;

    /** 契约：window ≥ 1（fail-fast——窗口宽以 tick 计）。 */
    public ExponentialWindowCounter(long window) {
        if (window < 1) {
            throw new IllegalArgumentException("window 须 ≥ 1：" + window);
        }
        this.window = window;
    }

    /** 当前 tick（只读面——时间推进对账用）。 */
    public synchronized long now() {
        return now;
    }

    /** 在当前 tick 记一个事件。 */
    public synchronized void insert() {
        buckets.add(new Bucket(1L, now, now));
        mergeByCapacity();
        evictExpired();
    }

    /** 时间推进一步（无事件）。 */
    public synchronized void tick() {
        now++;
        evictExpired();
    }

    /**
     * 最近 window 个 tick 的事件数估计——全界内桶（first &gt; now−window）
     * 计全，跨界桶（first ≤ now−window &lt; last）计 floor(capacity/2)。
     * 每跨界桶偏差 ≤ ceil(capacity/2)（见 {@link #errorBound()}）。
     */
    public synchronized long estimate() {
        long sum = 0L;
        for (Bucket b : buckets) {
            if (b.firstTimestamp() > now - window) {
                sum += b.capacity();
            } else if (b.lastTimestamp() > now - window) {
                sum += b.capacity() / 2L;
            }
        }
        return sum;
    }

    /** 当前误差上界读数 = Σ跨界桶 ceil(capacity/2)（无跨界即 0——估计精确）。 */
    public synchronized long errorBound() {
        long bound = 0L;
        for (Bucket b : buckets) {
            if (b.firstTimestamp() <= now - window && b.lastTimestamp() > now - window) {
                bound += (b.capacity() + 1L) / 2L;
            }
        }
        return bound;
    }

    /** 桶数读数（空间账——O(log) 增长 vs 事件数线性）。 */
    public synchronized int bucketCount() {
        return buckets.size();
    }

    /** 同容量至多 2 桶：新 1 桶可能使某容量到 3 个——合并最老两个（容量翻倍、跨度并接），set 回最老原位保持桶序。 */
    private void mergeByCapacity() {
        boolean merged = true;
        while (merged) {
            merged = false;
            for (long cap : new TreeSet<>(buckets.stream().map(Bucket::capacity).toList())) {
                int first = -1;
                int second = -1;
                int count = 0;
                for (int i = 0; i < buckets.size(); i++) {
                    if (buckets.get(i).capacity() == cap) {
                        count++;
                        if (first < 0) {
                            first = i;
                        } else if (second < 0) {
                            second = i;
                        }
                    }
                }
                if (count >= 3) {
                    Bucket a = buckets.get(first);
                    Bucket b = buckets.get(second);
                    buckets.remove(second);
                    buckets.set(first,
                            new Bucket(cap * 2L, a.firstTimestamp(), b.lastTimestamp()));
                    merged = true;
                    break;
                }
            }
        }
    }

    /** 惰性清出：整体过期（last ≤ now − window）的桶丢弃；跨界桶保留待计半。 */
    private void evictExpired() {
        buckets.removeIf(b -> b.lastTimestamp() <= now - window);
    }
}
