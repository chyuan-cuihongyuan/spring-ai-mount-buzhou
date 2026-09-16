package io.github.chyuan_cuihongyuan.buzhou.guard.decision;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 判定决策缓存（spec 2010 / T3121 / impl 1561）——OPA/Cedar decision
 * cache 思想：同输入判定在 TTL 内短路（省重复判定成本），过期重判并
 * 计数；容量上限 LRU 驱逐（accessOrder——最近未用先出）。hits/misses/
 * expirations/evictions 四计数显形（命中率对账——缓存有效性证）。
 *
 * <p>synchronized 小临界区；时间由调用方传入（确定性可回放；get 的
 * now 早于 put 时间戳视为未过期——时钟回拨语义宽进，文档显式）。
 */
public final class DecisionCache<K, V> {

    private record Entry<V>(V verdict, long storedAtMillis) {
    }

    private final long ttlMillis;
    private final int maxEntries;
    private final LinkedHashMap<K, Entry<V>> entries;
    private long hits;
    private long misses;
    private long expirations;
    private long evictions;

    /** 契约：ttlMillis &gt; 0、maxEntries ≥ 1（fail-fast）。 */
    public DecisionCache(long ttlMillis, int maxEntries) {
        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("ttlMillis 须 > 0：" + ttlMillis);
        }
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries 须 ≥ 1：" + maxEntries);
        }
        this.ttlMillis = ttlMillis;
        this.maxEntries = maxEntries;
        this.entries = new LinkedHashMap<>(16, 0.75f, true); // accessOrder LRU
    }

    /** 查判定：TTL 内命中（hits++）；过期逐条惰性清除（expirations++，
     * 不计入 misses——过期非未见过）；未存过 misses++。 */
    public synchronized Optional<V> get(K key, long nowMillis) {
        if (key == null) {
            throw new IllegalArgumentException("key 不能为 null");
        }
        Entry<V> entry = entries.get(key);
        if (entry == null) {
            misses++;
            return Optional.empty();
        }
        if (nowMillis - entry.storedAtMillis() >= ttlMillis) {
            entries.remove(key);
            expirations++;
            return Optional.empty();
        }
        hits++;
        return Optional.of(entry.verdict());
    }

    /** 存判定（覆盖旧值——重判后回填）；超容量驱逐最久未用（evictions++）。 */
    public synchronized void put(K key, V verdict, long nowMillis) {
        if (key == null) {
            throw new IllegalArgumentException("key 不能为 null");
        }
        if (verdict == null) {
            throw new IllegalArgumentException("verdict 不能为 null");
        }
        entries.put(key, new Entry<>(verdict, nowMillis));
        while (entries.size() > maxEntries) {
            K eldest = entries.keySet().iterator().next();
            entries.remove(eldest);
            evictions++;
        }
    }

    /** 显式失效（判定依据变更时——如策略热更新后全量重判前的精准失效）。 */
    public synchronized void invalidate(K key) {
        if (key == null) {
            throw new IllegalArgumentException("key 不能为 null");
        }
        entries.remove(key);
    }

    /** 当前缓存条数（容量水位对账面）。 */
    public synchronized int size() {
        return entries.size();
    }

    /** 四计数读数：hits/misses/expirations/evictions（命中率缓存有效性证）。 */
    public synchronized CacheStats stats() {
        return new CacheStats(hits, misses, expirations, evictions, entries.size());
    }

    /** 缓存对账快照。 */
    public record CacheStats(long hits, long misses, long expirations,
                             long evictions, int size) {

        /** 命中率（总查询为 0 时 0——空缓存不除零）。 */
        public double hitRate() {
            long total = hits + misses + expirations;
            return total == 0 ? 0.0d : (double) hits / total;
        }
    }
}
