package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 2Q 双队列缓存（spec 3033 / T5067 / impl 2034）——Two-Queue 思想
 * （Johnson & Shasha 1994，简化 2Q 无幽灵版）：**入口 FIFO**（A1in
 * 新面孔观察窗）+ **主 LRU**（Am 二触晋升区）——一次性扫描流只在
 * 入口 FIFO 自旋淘汰（扫描抗性——LRU 全 cache 被冲刷病的另一解），
 * 二次触达才值得占主 LRU（「热不热看第二触」的分诊思想）。与
 * ARC（幽灵自适应）同族异构：2Q 定比分（inbound/main 静态）、
 * ARC 自适应——按工作集稳定性选型。
 *
 * <p>泛型 K/V；总容量 = 入口+主（构造期定比）；双逐出计数对账；
 * 非线程安全。
 */
public final class TwoQueueCache<K, V> {

    private final int inboundCapacity;
    private final int mainCapacity;
    private final Map<K, V> inbound;   // A1in：FIFO（插入序）
    private final Map<K, V> main;      // Am：LRU（命中重排）
    private long inboundEvictions;
    private long mainEvictions;

    /** 总容量 ≥2，入口容量 ∈[1, 总−1]（主容量 = 总−入口）。 */
    public TwoQueueCache(int capacity, int inboundCapacity) {
        if (capacity < 2 || inboundCapacity < 1 || inboundCapacity >= capacity) {
            throw new IllegalArgumentException("capacity ≥ 2 且 1 ≤ inbound < capacity："
                    + capacity + "/" + inboundCapacity);
        }
        this.inboundCapacity = inboundCapacity;
        this.mainCapacity = capacity - inboundCapacity;
        this.inbound = new LinkedHashMap<>();
        this.main = new LinkedHashMap<>();
    }

    /** 命中：Am 内 LRU 重排；A1in 内**晋升 Am**（二触分诊）；未命中 null。 */
    public V get(K key) {
        V value = main.remove(key);
        if (value != null) {
            main.put(key, value);
            return value;
        }
        value = inbound.remove(key);
        if (value != null) {
            promote(key, value);
            return value;
        }
        return null;
    }

    /** 插入/更新：Am 更新重排；A1in 内晋升；新键入 A1in（FIFO 满则淘汰）。 */
    public void put(K key, V value) {
        if (main.containsKey(key)) {
            main.remove(key);
            main.put(key, value);
            return;
        }
        if (inbound.containsKey(key)) {
            inbound.remove(key);
            promote(key, value);
            return;
        }
        if (inbound.size() >= inboundCapacity) {
            K oldest = inbound.keySet().iterator().next();
            inbound.remove(oldest);
            inboundEvictions++;
        }
        inbound.put(key, value);
    }

    /** 实存条目数（入口+主）。 */
    public int size() {
        return inbound.size() + main.size();
    }

    /** 总容量（入口+主）。 */
    public int capacity() {
        return inboundCapacity + mainCapacity;
    }

    /** 入口（A1in）容量。 */
    public int inboundCapacity() {
        return inboundCapacity;
    }

    /** 入口逐出计数（对账面）。 */
    public long inboundEvictions() {
        return inboundEvictions;
    }

    /** 主区逐出计数（对账面）。 */
    public long mainEvictions() {
        return mainEvictions;
    }

    private void promote(K key, V value) {
        main.put(key, value);
        if (main.size() > mainCapacity) {
            K lru = main.keySet().iterator().next();
            main.remove(lru);
            mainEvictions++;
        }
    }
}
