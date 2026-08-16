package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.springframework.ai.chat.model.ChatResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 精确响应缓存存储（spec 53 §D / T206）：进程内 LRU + TTL 惰性过期
 * （{@link CachedEmbeddingProvider} 同风格：单锁 LinkedHashMap accessOrder——
 * 缓存读写为内存级低耗时，锁竞争可忽略）。
 *
 * <p>命中/未命中/逐出计数可观测（{@link #hitCount()}/{@link #missCount()}/
 * {@link #evictedCount()}）——成本护栏不静默。TTL 过期在命中路径惰性判定
 * （无后台线程）；容量逐出由 LRU 谓词驱动；两者同计 evicted。
 */
public final class ResponseCacheStore {

    private final int maxEntries;
    private final Duration ttl;
    private final Clock clock;
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();

    private record Entry(ChatResponse response, Instant expireAt) {
    }

    private final LinkedHashMap<String, Entry> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
            boolean evict = size() > maxEntries;
            if (evict) {
                evictions.incrementAndGet();
            }
            return evict;
        }
    };

    public ResponseCacheStore(int maxEntries, Duration ttl) {
        this(maxEntries, ttl, Clock.systemUTC());
    }

    public ResponseCacheStore(int maxEntries, Duration ttl, Clock clock) {
        if (maxEntries < 1) {
            throw new IllegalArgumentException("response-cache.max-entries 必须 >= 1（当前 " + maxEntries + "）");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("response-cache.ttl 必须为正时长（当前 " + ttl + "）");
        }
        this.maxEntries = maxEntries;
        this.ttl = ttl;
        this.clock = clock;
    }

    /** 命中查询（惰性过期：过期即弃 + evicted 计数 + miss 口径）。 */
    public Optional<ChatResponse> get(String key) {
        synchronized (cache) {
            Entry entry = cache.get(key);
            if (entry == null) {
                misses.incrementAndGet();
                return Optional.empty();
            }
            if (clock.instant().isAfter(entry.expireAt())) {
                cache.remove(key);
                evictions.incrementAndGet();
                misses.incrementAndGet();
                return Optional.empty();
            }
            hits.incrementAndGet();
            return Optional.of(entry.response());
        }
    }

    /** 写入（键已存在 = 刷新 expireAt）。 */
    public void put(String key, ChatResponse response) {
        if (key == null || response == null) {
            return;
        }
        synchronized (cache) {
            cache.put(key, new Entry(response, clock.instant().plus(ttl)));
        }
    }

    public long hitCount() {
        return hits.get();
    }

    public long missCount() {
        return misses.get();
    }

    public long evictedCount() {
        return evictions.get();
    }

    public int size() {
        synchronized (cache) {
            return cache.size();
        }
    }
}
