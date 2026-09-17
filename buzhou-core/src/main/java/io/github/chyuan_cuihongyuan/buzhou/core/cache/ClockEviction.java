package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import java.util.HashMap;
import java.util.Map;

/**
 * CLOCK 二次机会驱逐缓存（spec 3028 / T5057 / impl 2029）——
 * CLOCK/second-chance 思想（近似 LRU 的 O(1) 硬件页替换经典）：
 * 环形帧阵列+引用位——命中置位；满载逐出时针扫描：位 1 者**二次
 * 机会**（清位跳过），位 0 者逐出——访问近序不被一刀切（FIFO 盲
 * 逐出病的根治），又免 LRU 链表逐次搬移（每命中 O(1) 不移动）。
 * LRU 近似件：省链表维护、容忍近似序（热点识别不差分毫的场景）。
 *
 * <p>泛型 K/V；容量 ≥1；evictedCount 对账读数；非线程安全。
 */
public final class ClockEviction<K, V> {

    /** 帧槽（键值+引用位）。 */
    private static final class Frame<K, V> {
        K key;
        V value;
        boolean referenced;

        Frame(K key, V value) {
            this.key = key;
            this.value = value;
            this.referenced = true;
        }
    }

    private final Frame<K, V>[] frames;
    private final Map<K, Integer> slotOf = new HashMap<>();
    private int hand;
    private int occupied;
    private long evictedCount;

    /** 容量 ≥1。 */
    @SuppressWarnings("unchecked")
    public ClockEviction(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity ≥ 1：" + capacity);
        }
        this.frames = new Frame[capacity];
    }

    /** 命中（置引用位）；未命中 null。 */
    public V get(K key) {
        Integer slot = slotOf.get(key);
        if (slot == null) {
            return null;
        }
        Frame<K, V> frame = frames[slot];
        frame.referenced = true;
        return frame.value;
    }

    /** 插入/更新：满载时时针扫描——位 1 清位跳过（二次机会），位 0 逐出。 */
    public void put(K key, V value) {
        Integer slot = slotOf.get(key);
        if (slot != null) {
            Frame<K, V> frame = frames[slot];
            frame.value = value;
            frame.referenced = true;
            return;
        }
        if (occupied < frames.length) {
            frames[hand] = new Frame<>(key, value);
            slotOf.put(key, hand);
            occupied++;
            advanceHand();
            return;
        }
        while (frames[hand].referenced) {
            frames[hand].referenced = false;
            advanceHand();
        }
        Frame<K, V> victim = frames[hand];
        slotOf.remove(victim.key);
        evictedCount++;
        frames[hand] = new Frame<>(key, value);
        slotOf.put(key, hand);
        advanceHand();
    }

    /** 实存条目数。 */
    public int size() {
        return occupied;
    }

    /** 容量。 */
    public int capacity() {
        return frames.length;
    }

    /** 累计逐出数（对账读数）。 */
    public long evictedCount() {
        return evictedCount;
    }

    private void advanceHand() {
        hand = (hand + 1) % frames.length;
    }
}
