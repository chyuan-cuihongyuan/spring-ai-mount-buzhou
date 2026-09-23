package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Map;
import java.util.TreeMap;

/**
 * MemTable 内存表（spec 5024 / T6149 / impl 2175）——LSM
 * memtable 思想：可变有序内存表，`put` upsert（同键覆盖）、
 * 表满**拒写**返回 false（滚动交接责任在调用方——drain 后
 * 可再写）；`drain()` 整表按 key 字典序导出并清空（滚动到
 * 不可变段的确定性交接点）。无限累积（内存爆炸）与满即
 * 阻塞（无滚动语义）的病解。
 *
 * <p>与 SparseIndex（S22 不可变段索引）同族不同面：可变写侧
 * vs 不可变读侧。
 */
public final class MemTable {

    private final int maxEntries;
    private final TreeMap<String, String> table = new TreeMap<>();

    /** 定构（maxEntries ≤0 fail-fast）。 */
    public MemTable(int maxEntries) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries>0：" + maxEntries);
        }
        this.maxEntries = maxEntries;
    }

    /** 写入（upsert；满返回 false 不覆盖已有；null 键值 fail-fast）。 */
    public boolean put(String key, String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("key/value 非 null");
        }
        if (!table.containsKey(key) && table.size() >= maxEntries) {
            return false;   // 满——滚动责任在调用方
        }
        table.put(key, value);
        return true;
    }

    /** 读取（未命中 null）。 */
    public String get(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key 非 null");
        }
        return table.get(key);
    }

    /** 表是否已满。 */
    public boolean isFull() {
        return table.size() >= maxEntries;
    }

    /** 表项数读数。 */
    public int size() {
        return table.size();
    }

    /** 容量读数。 */
    public int maxEntries() {
        return maxEntries;
    }

    /** 整表导出（key 字典序）并清空——滚动交接点。 */
    public Map<String, String> drain() {
        java.util.NavigableMap<String, String> exported = new TreeMap<>(table);
        table.clear();
        return java.util.Collections.unmodifiableNavigableMap(exported);
    }
}
