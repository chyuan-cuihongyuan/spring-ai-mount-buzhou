package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SIEVE 缓存驱逐（spec 5015 / T6131 / impl 2166）——SIEVE 思想
 * （2024 论文）：插入序 FIFO 环 + 访问位——命中只置位**不重排**
 * （lazy promotion，热路径零搬移）；驱逐指针自上次停点扫：
 * 访问位=1 清位跳过、=0 摘除（一次保护机会，指针停在驱逐点
 * 之后）。LRU 每次命中搬移重排（热路径开销 + 扫描污染）的病解。
 *
 * <p>与 ClockSweepCache（spec 5009）同族不同面：访问位一次清位
 * vs 使用计数衰减。
 */
public final class SieveCache<K, V> {

    private final int capacity;
    private final Map<K, V> values = new HashMap<>();
    private final List<K> order = new ArrayList<>();
    private final Map<K, Boolean> visited = new HashMap<>();
    private int hand;

    /** 定构（capacity ≤0 fail-fast）。 */
    public SieveCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity>0：" + capacity);
        }
        this.capacity = capacity;
    }

    /** 装入（新项追加 FIFO 尾、访问位清零；满则先驱逐；null 值 fail-fast）。 */
    public void put(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (values.containsKey(key)) {
            values.put(key, value);
            return;
        }
        if (values.size() >= capacity) {
            evict();
        }
        values.put(key, value);
        order.add(key);
        visited.put(key, false);
    }

    /** 命中读取（置访问位、不重排；未命中 null）。 */
    public V get(K key) {
        Objects.requireNonNull(key, "key");
        V value = values.get(key);
        if (value != null) {
            visited.put(key, true);   // lazy promotion——只置位
        }
        return value;
    }

    /** 是否驻留（不置位）。 */
    public boolean containsKey(K key) {
        return values.containsKey(key);
    }

    /** 驻留数读数。 */
    public int size() {
        return values.size();
    }

    /** 容量读数。 */
    public int capacity() {
        return capacity;
    }

    /** FIFO 环序读数（确定性审计面；命中不重排的直接证据）。 */
    public List<K> order() {
        return List.copyOf(order);
    }

    /** 驱逐指针读数。 */
    public int hand() {
        return hand;
    }

    /** SIEVE 驱逐：清位跳过 / 零位摘除（指针停在驱逐点之后）。 */
    private void evict() {
        while (true) {
            if (hand >= order.size()) {
                hand = 0;
            }
            K candidate = order.get(hand);
            if (Boolean.TRUE.equals(visited.get(candidate))) {
                visited.put(candidate, false);   // 一次保护机会——清位
                hand++;
                continue;
            }
            values.remove(candidate);
            visited.remove(candidate);
            order.remove(hand);
            return;   // 指针停在驱逐点之后
        }
    }
}
