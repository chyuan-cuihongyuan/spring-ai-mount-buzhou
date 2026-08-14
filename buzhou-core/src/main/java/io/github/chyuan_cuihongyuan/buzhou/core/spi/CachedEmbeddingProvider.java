package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * embed-once 装饰器（impl-38 / spec 13 §growth-8；pgvector 22.6K★/Milvus 45.6K★
 * 「同内容只 embed 一次」语义）：内容 sha256 为键的进程内 LRU 缓存，装饰任意
 * {@link EmbeddingProvider}。recall_search / EpisodeLedger / SemanticChunkIndex
 * 共用<b>同一实例</b>即共享一份缓存（memory 模块在 provider 解析点统一包裹）。
 *
 * <p>命中/未命中计数可观测（{@link #hitCount()}/{@link #missCount()}）——成本护栏不静默。
 * 线程安全：单锁 LinkedHashMap LRU（embed 是低频重调用，锁竞争可忽略）。
 */
public final class CachedEmbeddingProvider implements EmbeddingProvider {

    /** 默认 LRU 容量（spec 13 §growth-8：内容 hash 键、LRU 容量默认 512）。 */
    public static final int DEFAULT_CAPACITY = 512;

    private final EmbeddingProvider delegate;
    private final int capacity;
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    private final LinkedHashMap<String, float[]> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
            return size() > capacity;
        }
    };

    public CachedEmbeddingProvider(EmbeddingProvider delegate) {
        this(delegate, DEFAULT_CAPACITY);
    }

    public CachedEmbeddingProvider(EmbeddingProvider delegate, int capacity) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate 不能为空");
        }
        this.delegate = delegate;
        this.capacity = capacity <= 0 ? DEFAULT_CAPACITY : capacity;
    }

    @Override
    public float[] embed(String text) {
        String key = sha256(text == null ? "" : text);
        synchronized (cache) {
            float[] cached = cache.get(key);
            if (cached != null) {
                hits.incrementAndGet();
                return cached;
            }
        }
        misses.incrementAndGet();
        float[] vector = delegate.embed(text);
        if (vector != null) {
            synchronized (cache) {
                cache.put(key, vector.clone());
            }
        }
        return vector;
    }

    public long hitCount() {
        return hits.get();
    }

    public long missCount() {
        return misses.get();
    }

    /** 在册缓存条数（测试与运维可观测）。 */
    public int cachedCount() {
        synchronized (cache) {
            return cache.size();
        }
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            // SHA-256 必在；兜底退化原始键（不因摘要算法缺失丢缓存语义）
            return text;
        }
    }
}
