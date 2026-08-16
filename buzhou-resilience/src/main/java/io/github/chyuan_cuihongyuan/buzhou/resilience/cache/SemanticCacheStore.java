package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.springframework.ai.chat.model.ChatResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 语义缓存存储（spec 55 §A / T240 / effort#15，LiteLLM semantic caching 同思想——本地
 * 裁定：进程内向量线性扫描）：条目 =（embedding 向量、终态响应、expireAt、桶键）；
 * 查询 = embed 后<b>桶内</b>线性 cosine 最近邻 ≥ 阈值即命中。
 *
 * <p>桶键 = modelName + options 采样（{@link ResponseCacheKeys#optionsSample} 同口径，
 * 调用方拼装）——跨模型/参数变体天然隔离，不进入相似度比较。条目量级数百（FAQ 型负载
 * + 容量上限），线性扫描量级由 perf 哨兵钉住。
 *
 * <p>LRU + TTL 惰性过期（{@link ResponseCacheStore} 同风格：单锁 LinkedHashMap
 * accessOrder；TTL 命中路径惰性判定，无后台线程）；hit/miss/evicted 计数可观测。
 * cosine 零向量防护（范数 0 → 相似度 0 不 NaN）；维度不匹配条目防御性跳过。
 */
public final class SemanticCacheStore {

    private final int maxEntries;
    private final Duration ttl;
    private final double threshold;
    private final Clock clock;
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();

    private record Entry(String bucket, float[] embedding, ChatResponse response, Instant expireAt) {
    }

    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
            boolean evict = size() > maxEntries;
            if (evict) {
                evictions.incrementAndGet();
            }
            return evict;
        }
    };

    private long seq;

    public SemanticCacheStore(int maxEntries, Duration ttl, double threshold) {
        this(maxEntries, ttl, threshold, Clock.systemUTC());
    }

    public SemanticCacheStore(int maxEntries, Duration ttl, double threshold, Clock clock) {
        if (maxEntries < 1) {
            throw new IllegalArgumentException("semantic-cache.max-entries 必须 >= 1（当前 " + maxEntries + "）");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("semantic-cache.ttl 必须为正时长（当前 " + ttl + "）");
        }
        if (!(threshold > 0.0 && threshold <= 1.0)) {
            throw new IllegalArgumentException(
                    "semantic-cache.similarity-threshold 必须在 (0,1]（当前 " + threshold + "）");
        }
        this.maxEntries = maxEntries;
        this.ttl = ttl;
        this.threshold = threshold;
        this.clock = clock;
    }

    /**
     * 桶内最近邻查询：cosine 相似度 ≥ 阈值的最近条目命中（accessOrder 触达）；
     * 无桶/无达标条目 = miss。过期条目惰性清除（同计 evicted）。
     */
    public synchronized Optional<ChatResponse> findNearest(String bucket, float[] queryEmbedding) {
        if (bucket == null || queryEmbedding == null || queryEmbedding.length == 0) {
            misses.incrementAndGet();
            return Optional.empty();
        }
        purgeExpired();
        String bestKey = null;
        double bestScore = 0;
        Entry best = null;
        for (Map.Entry<String, Entry> e : entries.entrySet()) {
            Entry entry = e.getValue();
            if (!entry.bucket().equals(bucket)) {
                continue;
            }
            double score = cosine(queryEmbedding, entry.embedding());
            if (score >= threshold && score >= bestScore) {
                bestScore = score;
                bestKey = e.getKey();
                best = entry;
            }
        }
        if (best == null) {
            misses.incrementAndGet();
            return Optional.empty();
        }
        entries.get(bestKey); // 触达 LRU accessOrder
        hits.incrementAndGet();
        return Optional.of(best.response());
    }

    /** 写入（调用方保证终态边界——带 toolCalls 不写；同 query 文本重复写允许共存，LRU 自然收敛）。 */
    public synchronized void put(String bucket, float[] embedding, ChatResponse response) {
        if (bucket == null || embedding == null || embedding.length == 0 || response == null) {
            return;
        }
        entries.put(bucket + "#" + (seq++), new Entry(bucket, embedding, response,
                clock.instant().plus(ttl)));
    }

    /** 相似度阈值（观测/测试面）。 */
    public double threshold() {
        return threshold;
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

    public synchronized int size() {
        purgeExpired();
        return entries.size();
    }

    private void purgeExpired() {
        Instant now = clock.instant();
        entries.values().removeIf(entry -> {
            boolean expired = now.isAfter(entry.expireAt());
            if (expired) {
                evictions.incrementAndGet();
            }
            return expired;
        });
    }

    /** cosine 相似度（零向量 → 0；维度不匹配 → 0——防御坏数据，不抛）。 */
    static double cosine(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
