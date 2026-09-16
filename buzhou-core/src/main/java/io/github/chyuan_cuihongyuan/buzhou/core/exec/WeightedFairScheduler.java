package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 加权公平调度器（spec 2026 / T3153 / impl 1577）——网络调度 DRR
 *（Deficit Round Robin）思想：每流权重 + 积分账户轮询——轮到流时
 * deficit += quantum × weight，按每项 cost=1 服务到 deficit 不足或流
 * 空；空流积分清零出活跃环（防积累特权），再入从零起步。长期服务比
 * ≈ 权重比（加权 max-min 公平），高权重多得但不独占。
 *
 * <p>synchronized 小临界区；轮询顺序稳定（流注册序）。
 */
public final class WeightedFairScheduler<T> {

    /** 默认每轮量子（积分/权重·轮）——每项 cost 1 下 quantum=1 即每权重点一项。 */
    public static final int DEFAULT_QUANTUM = 1;

    private final int quantum;
    private final Map<String, Integer> weights = new LinkedHashMap<>(); // 注册序稳定
    private final Map<String, Deque<T>> queues = new HashMap<>();
    private final Map<String, Long> deficit = new HashMap<>();
    private final List<String> active = new ArrayList<>(); // 活跃环（非空流）
    private int activeCursor;
    private final Map<String, Long> servedByStream = new HashMap<>();
    private String currentStream; // 粘性轮持有者（轮内消费不重入账）

    /** 契约：quantum ≥ 1（fail-fast）。 */
    public WeightedFairScheduler(int quantum) {
        if (quantum < 1) {
            throw new IllegalArgumentException("quantum 须 ≥ 1：" + quantum);
        }
        this.quantum = quantum;
    }

    public WeightedFairScheduler() {
        this(DEFAULT_QUANTUM);
    }

    /** 注册流（权重 ≥ 1——0 权永不服务，fail-fast 防配置错误）。契约：id 非空非重复。 */
    public synchronized void registerStream(String streamId, int weight) {
        if (streamId == null || streamId.isBlank()) {
            throw new IllegalArgumentException("streamId 不能为空");
        }
        if (weight < 1) {
            throw new IllegalArgumentException("weight 须 ≥ 1：" + weight);
        }
        if (weights.containsKey(streamId)) {
            throw new IllegalArgumentException("流已注册：" + streamId);
        }
        weights.put(streamId, weight);
        queues.put(streamId, new ArrayDeque<>());
    }

    /** 入队（未注册流 fail-fast）；空流转入活跃环。 */
    public synchronized void enqueue(String streamId, T item) {
        if (item == null) {
            throw new IllegalArgumentException("item 不能为 null");
        }
        Deque<T> queue = queues.get(streamId);
        if (queue == null) {
            throw new IllegalArgumentException("流未注册：" + streamId);
        }
        if (queue.isEmpty()) {
            deficit.put(streamId, 0L); // 空流再入从零起步（防积累特权）
            active.add(streamId);
        }
        queue.addLast(item);
    }

    /**
     * DRR 轮询出一个元素（粘性轮内消费）：当前流积分可负担则继续从
     * 该流出队（轮内不再入账——权重以突发形态兑现，长期服务比 ≈
     * 权重比）；不足/空则让出游标，下一活跃流入账 quantum×weight 后
     * 消费。无可服务元素返 null。
     */
    public synchronized T pollNext() {
        if (active.isEmpty()) {
            return null;
        }
        // 粘性：当前流轮内继续消费（无入账）
        if (currentStream != null) {
            T sticky = consumeIfAffordable(currentStream);
            if (sticky != null) {
                return sticky;
            }
            advancePast(currentStream); // 用尽/空——让出
        }
        // 轮转：下一活跃流入账并消费
        int attempts = 0;
        while (attempts < active.size() && !active.isEmpty()) {
            if (activeCursor >= active.size()) {
                activeCursor = 0;
            }
            String stream = active.get(activeCursor);
            if (queues.get(stream).isEmpty()) {
                retireStream(stream);
                continue;
            }
            long newDeficit = deficit.get(stream) + (long) quantum * weights.get(stream);
            deficit.put(stream, newDeficit);
            currentStream = stream;
            T item = consumeIfAffordable(stream);
            if (item != null) {
                return item;
            }
            advancePast(stream); // 入账后仍不足（理论不达：quantum×weight≥1）——防御
            attempts++;
        }
        currentStream = null;
        return null;
    }

    /** 轮内消费：积分 ≥ 1 且队列非空则出一项（积分 −1；服务至空即退休）。 */
    private T consumeIfAffordable(String stream) {
        Deque<T> queue = queues.get(stream);
        long d = deficit.get(stream);
        if (d < 1 || queue.isEmpty()) {
            return null;
        }
        deficit.put(stream, d - 1);
        servedByStream.merge(stream, 1L, Long::sum);
        T item = queue.pollFirst();
        if (queue.isEmpty()) {
            retireStream(stream);
            currentStream = null;
        }
        return item;
    }

    /** 让出当前流：游标推进（退休流已在 consume 内处理）。 */
    private void advancePast(String stream) {
        currentStream = null;
        int idx = active.indexOf(stream);
        if (idx >= 0) {
            activeCursor = idx + 1; // 下一轮询起点
            if (activeCursor >= active.size()) {
                activeCursor = 0;
            }
        }
    }

    /** 各流累计服务数（长期公平对账面——服务比应≈权重比）。 */
    public synchronized Map<String, Long> servedByStream() {
        return new LinkedHashMap<>(servedByStream);
    }

    /** 活跃（非空）流数。 */
    public synchronized int activeStreamCount() {
        return active.size();
    }

    private void retireStream(String stream) {
        active.remove(activeCursor);
        if (activeCursor >= active.size()) {
            activeCursor = 0;
        }
        deficit.put(stream, 0L);
    }
}
