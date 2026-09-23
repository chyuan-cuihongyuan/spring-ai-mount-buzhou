package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Clock-Sweep 缓存驱逐（spec 5009 / T6119 / impl 2160）——
 * PostgreSQL Buffer Manager clock-sweep 思想：环形帧序 + 时钟
 * 指针，驱逐自指针起扫描——使用计数 {@code usage=0} 即摘除、
 * {@code usage>0} 衰减后跳过（**第二机会**：常用页多扛几轮但
 * 不豁免）；命中 +1、封顶 {@value #MAX_USAGE}（防老页计数
 * 奇高永驻）。纯 LRU（偶发扫描洗出热页）与 LFU 全量计数
 * （老页永驻）的病解；确定性驱动（无时间依赖）。
 *
 * <p>与精确响应缓存（spec 53，LRU+TTL）同族不同面。
 */
public final class ClockSweepCache<K, V> {

    /** 使用计数封顶（教学钳制——PostgreSQL 真实口径为 INT_MAX）。 */
    private static final int MAX_USAGE = 3;

    private final int capacity;
    private final Map<K, Frame> frames = new HashMap<>();
    private final List<K> ring = new ArrayList<>();
    private int clockHand;

    private static final class Frame<V> {
        private V value;
        private int usage;
    }

    /** 定构（capacity ≤0 fail-fast）。 */
    public ClockSweepCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity>0：" + capacity);
        }
        this.capacity = capacity;
    }

    /** 装入/覆盖（新帧 usage=1；覆盖只更新值，帧位与计数不变）。 */
    public void put(K key, V value) {
        Objects.requireNonNull(key, "key");
        Frame<V> frame = frames.get(key);
        if (frame != null) {
            frame.value = value;
            return;
        }
        if (frames.size() >= capacity) {
            evictOne();
        }
        Frame<V> fresh = new Frame<>();
        fresh.value = value;
        fresh.usage = 1;
        frames.put(key, fresh);
        ring.add(key);
    }

    /** 命中读取（usage+1 封顶；未命中 null）。 */
    public V get(K key) {
        Objects.requireNonNull(key, "key");
        Frame<V> frame = frames.get(key);
        if (frame == null) {
            return null;
        }
        frame.usage = Math.min(frame.usage + 1, MAX_USAGE);
        return frame.value;
    }

    /** 是否驻留（不影响使用计数）。 */
    public boolean containsKey(K key) {
        return frames.containsKey(key);
    }

    /**
     * 驱逐一帧（时钟指针扫描：usage=0 摘除、>0 衰减跳过；
     * 空缓存返回 null）。
     *
     * @return 被驱逐键（或 null）
     */
    public K evictOne() {
        if (frames.isEmpty()) {
            return null;
        }
        int scanned = 0;
        while (scanned <= ring.size()) {   // 一圈内必终止（usage 有上限必归零）
            if (clockHand >= ring.size()) {
                clockHand = 0;
            }
            K candidate = ring.get(clockHand);
            Frame<V> frame = frames.get(candidate);
            if (frame == null) {
                ring.remove(clockHand);   // 已被 put 路径摘除的残位
                continue;
            }
            if (frame.usage == 0) {
                frames.remove(candidate);
                ring.remove(clockHand);
                clockHand = clockHand >= ring.size() ? 0 : clockHand;
                return candidate;
            }
            frame.usage--;
            clockHand++;
            scanned++;
        }
        // 理论不可达（衰减必归零）——保守摘除指针位
        K fallback = ring.get(Math.min(clockHand, ring.size() - 1));
        frames.remove(fallback);
        ring.remove(fallback);
        return fallback;
    }

    /** 驻留数读数。 */
    public int size() {
        return frames.size();
    }

    /** 容量读数。 */
    public int capacity() {
        return capacity;
    }

    /** 时钟指针读数。 */
    public int clockHand() {
        return clockHand;
    }

    /** 环上键序读数（确定性审计面）。 */
    public java.util.List<K> ringOrder() {
        return java.util.List.copyOf(ring);
    }
}
