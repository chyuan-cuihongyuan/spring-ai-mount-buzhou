package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Segmented LRU 分段缓存（spec 5039 / T6179 / impl 2190）——
 * PostgreSQL 缓冲区管理/Caffeine SLRU 思想：全容量切
 * **试用期（probationary）**与**保护期（protected）**两段，
 * 段内各自 LRU——新键入试用期尾，命中一次即晋升保护期尾，
 * 保护期满把保护期头（最久未访问）**降级**回试用期尾，
 * 淘汰只从试用期头走——一次性扫描不再污染保护段（普适
 * LRU 一次全表扫把热数据全冲走的病解）。确定性无时间依赖。
 *
 * <p>与 ClockSweepCache（spec 5009）同族不同面：使用计数
 * 衰减 vs 双段晋升降级；与 SieveCache（spec 5015）不同面：
 * 访问位单环 vs 段间迁移。
 */
public final class SlruCache<K, V> {

    private static final double MIN_PROTECTED_RATIO_EXCLUSIVE = 0.0;

    private static final double MAX_PROTECTED_RATIO_EXCLUSIVE = 1.0;

    private final int capacity;
    private final int protectedCapacity;
    private final Map<K, V> probationary = new HashMap<>();
    private final Deque<K> probationaryOrder = new ArrayDeque<>();
    private final Map<K, V> protectedSegment = new HashMap<>();
    private final Deque<K> protectedOrder = new ArrayDeque<>();
    private long evictedCount;

    /** 定构（capacity≥1；protectedRatio∈(0,1) 定保护段配额）。 */
    public SlruCache(int capacity, double protectedRatio) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity≥1：" + capacity);
        }
        if (protectedRatio <= MIN_PROTECTED_RATIO_EXCLUSIVE
                || protectedRatio >= MAX_PROTECTED_RATIO_EXCLUSIVE) {
            throw new IllegalArgumentException("protectedRatio∈(0,1)：" + protectedRatio);
        }
        this.capacity = capacity;
        this.protectedCapacity = Math.max(1, (int) Math.round(capacity * protectedRatio));
    }

    /** 取值并晋升（试用命中→保护尾；保护命中→保护尾；缺席空）。 */
    public Optional<V> get(K key) {
        requireKey(key);
        V protectedValue = protectedSegment.get(key);
        if (protectedValue != null) {
            touchProtected(key);
            return Optional.of(protectedValue);
        }
        V probationaryValue = probationary.get(key);
        if (probationaryValue != null) {
            promote(key, probationaryValue);
            return Optional.of(probationaryValue);
        }
        return Optional.empty();
    }

    /** 缺席回退取值（无 null 面）。 */
    public V getOrDefault(K key, V fallback) {
        return get(key).orElse(fallback);
    }

    /** 放入/覆盖（已存在等同命中晋升；新键入试用尾；满则淘汰试用头）。 */
    public void put(K key, V value) {
        requireKey(key);
        if (value == null) {
            throw new IllegalArgumentException("value 非空");
        }
        if (protectedSegment.containsKey(key)) {
            protectedSegment.put(key, value);
            touchProtected(key);
            return;
        }
        if (probationary.containsKey(key)) {
            promote(key, value);
            return;
        }
        while (size() >= capacity) {
            evictProbationaryHead();
        }
        probationary.put(key, value);
        probationaryOrder.addLast(key);
    }

    /** 透视图（不晋升、不改序）。 */
    public boolean containsKey(K key) {
        requireKey(key);
        return probationary.containsKey(key) || protectedSegment.containsKey(key);
    }

    /** 全量键数读数。 */
    public int size() {
        return probationary.size() + protectedSegment.size();
    }

    /** 试用期键数读数。 */
    public int probationarySize() {
        return probationary.size();
    }

    /** 保护期键数读数。 */
    public int protectedSize() {
        return protectedSegment.size();
    }

    /** 已淘汰键数读数（缓存代价诚实可见）。 */
    public long evictedCount() {
        return evictedCount;
    }

    private void promote(K key, V value) {
        probationary.remove(key);
        probationaryOrder.remove(key);
        while (protectedSegment.size() >= protectedCapacity) {
            demoteProtectedHead();
        }
        protectedSegment.put(key, value);
        protectedOrder.addLast(key);
    }

    private void touchProtected(K key) {
        protectedOrder.remove(key);
        protectedOrder.addLast(key);
    }

    /** 保护期头（最久未访问）降级回试用尾。 */
    private void demoteProtectedHead() {
        K demoted = protectedOrder.pollFirst();
        V value = protectedSegment.remove(demoted);
        probationary.put(demoted, value);
        probationaryOrder.addLast(demoted);
    }

    private void evictProbationaryHead() {
        if (probationaryOrder.isEmpty()) {
            throw new IllegalStateException("试用段空不可淘汰（容量守恒破坏）");
        }
        K evicted = probationaryOrder.pollFirst();
        probationary.remove(evicted);
        evictedCount++;
    }

    private static void requireKey(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("key 非空");
        }
    }
}
