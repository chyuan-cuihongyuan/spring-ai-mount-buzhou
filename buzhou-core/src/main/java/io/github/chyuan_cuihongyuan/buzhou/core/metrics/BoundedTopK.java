package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * 有界 Top-K 收集器（spec 2055 / T3211 / impl 1606）——流式 top-K
 * 小顶堆思想：持续推入 (score, item)，只留分数前 K——堆顶是「守门员」
 *（现任第 K 名）：新值胜过守门员即逐守入门（O(log K) 每推入，内存
 * O(K) 恒定——流式榜单不必存全集）。慢调用榜/热点榜/大额账单榜的
 * 通用底座（与 ToolSlowLog 的 FIFO 榜互补：那按时间窗留尾，这按值
 * 留大）。
 *
 * <p>synchronized 小临界区；同分后进者胜（LIFO 平局——最新热点优先）。
 */
public final class BoundedTopK<T> {

    /** 榜单条目：分数 + 关联物。 */
    public record Entry<T>(double score, T item) {
    }

    private final int capacity;
    private final PriorityQueue<Entry<T>> heap; // 小顶——堆顶=守门员
    private long evicted;
    private long rejected; // 未达守门员的推入

    /** 契约：capacity ≥ 1（fail-fast）。 */
    public BoundedTopK(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity 须 ≥ 1：" + capacity);
        }
        this.capacity = capacity;
        this.heap = new PriorityQueue<>(Comparator.comparingDouble(Entry::score));
    }

    /** 推入：胜过守门员（或未满）即入榜并逐出前守门员（evicted 计数）；否则拒（rejected 计数）。 */
    public synchronized void offer(double score, T item) {
        if (item == null) {
            throw new IllegalArgumentException("item 不能为 null");
        }
        if (Double.isNaN(score)) {
            throw new IllegalArgumentException("score 不能为 NaN");
        }
        if (heap.size() < capacity) {
            heap.add(new Entry<>(score, item));
            return;
        }
        Entry<T> gatekeeper = heap.peek();
        if (score > gatekeeper.score()) { // 严格大于——同分守门员保位（先入者优先）
            heap.poll();
            evicted++;
            heap.add(new Entry<>(score, item));
        } else {
            rejected++;
        }
    }

    /** 榜单快照：分数降序（同分入榜序倒序——最新在前）。 */
    public synchronized List<Entry<T>> top() {
        List<Entry<T>> all = new ArrayList<>(heap);
        all.sort(Comparator.comparingDouble(Entry<T>::score).reversed());
        return all;
    }

    /** 当前守门员分数（第 K 名；未满 = 堆顶即最小；空 = −∞）。 */
    public synchronized double gatekeeperScore() {
        Entry<T> top = heap.peek();
        return top == null ? Double.NEGATIVE_INFINITY : top.score();
    }

    /** 榜内数。 */
    public synchronized int size() {
        return heap.size();
    }

    /** 对账双计数：被逐出（换防次数）/未入门（落选推入）。 */
    public synchronized TopKStats stats() {
        return new TopKStats(evicted, rejected, heap.size());
    }

    /** Top-K 账快照。 */
    public record TopKStats(long evicted, long rejected, int size) {
    }
}
