package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * LFU 频次放逐（spec 8021 / V8043 / impl 2323）——
 * O'Neil, Graefe & Koniassek 1993 思想（Redis allkeys-lfu
 * 同源）：**键→频次表 + 频次桶（LinkedHashSet——同频取先入
 * 桶者 canonical）**——get 命中升频、满容量逐「最低频次+
 * 最先入桶」键——LRU 只看时新性（偶发扫描把热键冲出）的
 * 病解。put upsert 覆值不增位、被逐键返回面（无逐出 null
 * 诚实）；peek 不升频读；null 键值/容量 <1 fail-fast；同
 * 操作序同放逐完全确定。
 *
 * <p>与 SieveCache/LruKEviction（同包）同族不同面：频次
 * 放逐 vs 时新性/插手位图放逐。
 */
public final class LfuEviction {

    private final int capacity;
    private final Map<String, String> values = new HashMap<>();
    private final Map<String, Integer> frequencies = new HashMap<>();
    private final Map<Integer, LinkedHashSet<String>> buckets = new HashMap<>();
    private int minFrequency;

    /** 容量 ≥1（越域 fail-fast）。 */
    public LfuEviction(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("容量非负（实际 " + capacity + "）");
        }
        this.capacity = capacity;
    }

    /** 读并升频（缺席 null 诚实缺省）。 */
    public String get(String key) {
        if (key == null) {
            throw new IllegalArgumentException("键非空引用");
        }
        if (!values.containsKey(key)) {
            return null;
        }
        bump(key);
        return values.get(key);
    }

    /** 不升频读（缺席 null 诚实缺省）。 */
    public String peek(String key) {
        if (key == null) {
            throw new IllegalArgumentException("键非空引用");
        }
        return values.get(key);
    }

    /** 放入（upsert 覆值不增位；满容量逐出时返回被逐键，否则 null）。 */
    public String put(String key, String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("键值均非空引用");
        }
        if (values.containsKey(key)) {
            values.put(key, value);
            bump(key);
            return null;
        }
        String evicted = null;
        if (values.size() == capacity) {
            LinkedHashSet<String> victimBucket = buckets.get(minFrequency);
            evicted = victimBucket.iterator().next();
            victimBucket.remove(evicted);
            if (victimBucket.isEmpty()) {
                buckets.remove(minFrequency);
            }
            values.remove(evicted);
            frequencies.remove(evicted);
        }
        values.put(key, value);
        frequencies.put(key, 1);
        buckets.computeIfAbsent(1, f -> new LinkedHashSet<>()).add(key);
        minFrequency = 1;
        return evicted;
    }

    /** 键数。 */
    public int size() {
        return values.size();
    }

    private void bump(String key) {
        int frequency = frequencies.get(key);
        int next = frequency + 1;
        LinkedHashSet<String> bucket = buckets.get(frequency);
        bucket.remove(key);
        if (bucket.isEmpty()) {
            buckets.remove(frequency);
            if (minFrequency == frequency) {
                minFrequency = next;
            }
        }
        frequencies.put(key, next);
        buckets.computeIfAbsent(next, f -> new LinkedHashSet<>()).add(key);
    }
}
