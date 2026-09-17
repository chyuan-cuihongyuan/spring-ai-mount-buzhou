package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 * ARC 自适应替换缓存（spec 3009 / T5019 / impl 2010）——Megiddo &
 * Modha ARC 思想（IBM patent，比 LRU「聪明」的经典）：四链结构
 * T1（新近一次）/ T2（新近多次）+ B1/B2 幽灵链（已逐出的键），
 * 目标参数 p 自适应——幽灵命中在 B1（该多保新面孔）p 上调、在
 * B2（该多保热面孔）p 下调，**工作集在「新近敏感」与「频率敏感」
 * 之间自平衡**。一次性扫描流只污染 T1（扫描抗性——LRU 全 cache
 * 被冲刷病的根治）；反复命中的键晋升 T2 受保护。
 *
 * <p>容量 ≥1 校验；总实存 ≤ capacity；幽灵各 ≤ capacity；非线程
 * 安全（单线程口径，并发外包装归调用方）。
 */
public final class AdaptiveReplacementCache<K, V> {

    private final int capacity;
    private int p;

    private final LinkedHashMap<K, V> t1 = new LinkedHashMap<>();
    private final LinkedHashMap<K, V> t2 = new LinkedHashMap<>();
    private final LinkedHashSet<K> b1 = new LinkedHashSet<>();
    private final LinkedHashSet<K> b2 = new LinkedHashSet<>();

    /** 容量 ≥1（0 容量缓存无意义，fail-fast）。 */
    public AdaptiveReplacementCache(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity ≥ 1：" + capacity);
        }
        this.capacity = capacity;
    }

    /** 命中：T1/T2 均晋升 MRU-T2（频率轨道）；未命中返回 null。 */
    public V get(K key) {
        V value = t1.remove(key);
        if (value != null) {
            t2.put(key, value);
            return value;
        }
        if (t2.containsKey(key)) {
            value = t2.remove(key);
            t2.put(key, value);
            return value;
        }
        return null;
    }

    /** 插入/更新：全 miss 进 MRU-T1；幽灵命中自适应调 p 后进 T2。 */
    public void put(K key, V value) {
        if (t1.containsKey(key)) {
            t1.remove(key);
            t2.put(key, value);
            return;
        }
        if (t2.containsKey(key)) {
            t2.remove(key);
            t2.put(key, value);
            return;
        }
        if (b1.contains(key)) {
            int delta = Math.max(1, b2.size() / Math.max(1, b1.size()));
            p = Math.min(capacity, p + delta);
            b1.remove(key);
            replace();
            t2.put(key, value);
            return;
        }
        if (b2.contains(key)) {
            int delta = Math.max(1, b1.size() / Math.max(1, b2.size()));
            p = Math.max(0, p - delta);
            b2.remove(key);
            replace();
            t2.put(key, value);
            return;
        }
        // 全 miss：目录管理（原版 ARC case iv）
        int l1 = t1.size() + b1.size();
        if (l1 == capacity) {
            if (t1.size() < capacity) {
                b1.remove(lruOf(b1));
                replace();
            } else {
                // |T1| 独占 c：T1-LRU 整删（不入幽灵——保 |L1| ≤ c 不变量）
                t1.remove(lruOf(t1.keySet()));
            }
        } else if (l1 < capacity && size() + b1.size() + b2.size() >= capacity) {
            if (size() + b1.size() + b2.size() == 2 * capacity && !b2.isEmpty()) {
                b2.remove(lruOf(b2));
            }
            replace();
        }
        t1.put(key, value);
    }

    /** 实存条目数（T1+T2）。 */
    public int size() {
        return t1.size() + t2.size();
    }

    /** 容量。 */
    public int capacity() {
        return capacity;
    }

    /** T1 目标大小 p 的自适应读数（0..capacity——工作集平衡指针）。 */
    public int targetRecency() {
        return p;
    }

    /** T1（一次轨道）实存数。 */
    public int t1Size() {
        return t1.size();
    }

    /** T2（多次轨道）实存数。 */
    public int t2Size() {
        return t2.size();
    }

    /** B1（新近幽灵）数。 */
    public int b1Size() {
        return b1.size();
    }

    /** B2（频率幽灵）数。 */
    public int b2Size() {
        return b2.size();
    }

    /** REPLACE：T1 超 p 则逐 T1-LRU 入 B1，否则逐 T2-LRU 入 B2。 */
    private void replace() {
        if (!t1.isEmpty() && (t1.size() > p || (!b2.isEmpty() && t1.size() == p))) {
            K evicted = lruOf(t1.keySet());
            t1.remove(evicted);
            b1.add(evicted);
        } else if (!t2.isEmpty()) {
            K evicted = lruOf(t2.keySet());
            t2.remove(evicted);
            b2.add(evicted);
        }
    }

    private static <E> E lruOf(Iterable<E> ordered) {
        return ordered.iterator().next();
    }
}
