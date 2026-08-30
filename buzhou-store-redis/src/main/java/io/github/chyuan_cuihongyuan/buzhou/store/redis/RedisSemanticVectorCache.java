package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * 共享 Redis 语义向量缓存（spec 125 / T471，RediSearch 向量索引语义的可移植实现）：
 * 桶（modelName + options 采样，spec 55 同口径）= 一个 Redis HASH，field = entryId，
 * value = JSON（embedding / payload / expireAt / seq）——<b>跨实例共享</b>语义缓存，
 * 实例 A 学会的 FAQ 相似命中，实例 B 同义问法直接命中。
 *
 * <p>查询 = HGETALL 后<b>客户端 cosine 最近邻</b>（桶内量级数百，与进程内版
 * spec 55 同级成本；服务端 FT.SEARCH KNN 为演进入档路径）。写入 = 惰性清过期 →
 * 容量驱逐（最低 seq 最旧先出）→ HSET + 桶键 EXPIRE 刷新（双层过期：条目 expireAt
 * 惰性判定 + 桶键 TTL 兜底）。
 *
 * <p><b>故障语义（与限流后端刻意不对称）</b>：Redis 不可达 = 旁路 miss + bypass
 * 计数，<b>不 fail-fast</b>——缓存失效的风险是「多付一次模型调用」，限流失效的风险
 * 是「打爆上游」；限流 fail-fast（{@link RedisRateLimitBackend}）、缓存 fail-open
 * （本类）。hit / miss / bypass 三计数可观测。
 *
 * <p>cosine 防护与进程内版一致：零范数 → 相似度 0 不 NaN；维度不匹配条目跳过。
 */
public final class RedisSemanticVectorCache implements AutoCloseable {

    /** 命中结果（最近邻 ≥ 阈值）。 */
    public record Hit(String entryId, String payload, double similarity) {
    }

    /** 条目存储形态（JSON value）。 */
    private record Entry(float[] emb, String payload, long expireAtMillis, long seq) {
    }

    private final StatefulRedisConnection<String, String> connection;
    private final RedisCommands<String, String> commands;
    private final String keyPrefix;
    private final int maxEntriesPerBucket;
    private final double threshold;
    private long seq;

    /**
     * @param client              Lettuce 客户端（本类独占派生连接；client 生命周期归调用方）
     * @param keyPrefix           键前缀（空则 {@code buzhou:semvec:}）
     * @param maxEntriesPerBucket 桶容量上限（≥1；超出驱逐最旧 seq）
     * @param threshold           cosine 命中阈值（(0,1]，与 spec 55 默认 0.95 同域）
     */
    public RedisSemanticVectorCache(RedisClient client, String keyPrefix,
                                    int maxEntriesPerBucket, double threshold) {
        this(client.connect(), keyPrefix, maxEntriesPerBucket, threshold);
    }

    RedisSemanticVectorCache(StatefulRedisConnection<String, String> connection, String keyPrefix,
                             int maxEntriesPerBucket, double threshold) {
        if (maxEntriesPerBucket < 1) {
            throw new IllegalArgumentException(
                    "maxEntriesPerBucket 必须 >= 1（当前 " + maxEntriesPerBucket + "）");
        }
        if (!(threshold > 0.0 && threshold <= 1.0)) {
            throw new IllegalArgumentException("threshold 必须在 (0,1]（当前 " + threshold + "）");
        }
        this.connection = connection;
        this.commands = connection.sync();
        this.keyPrefix = keyPrefix == null || keyPrefix.isBlank() ? "buzhou:semvec:" : keyPrefix;
        this.maxEntriesPerBucket = maxEntriesPerBucket;
        this.threshold = threshold;
    }

    /**
     * 写入条目：惰性清过期 → 容量驱逐（≥ 上限时移除最低 seq）→ HSET + 桶键
     * EXPIRE = ttl（兜底回收空桶）。旁路语义：Redis 不可达时静默放弃写入（bypass 计数）。
     */
    public void put(String bucket, String entryId, float[] embedding, String payload, Duration ttl) {
        if (bucket == null || entryId == null || embedding == null || embedding.length == 0
                || ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("bucket/entryId/embedding/ttl 必须合法非空");
        }
        String key = bucketKey(bucket);
        try {
            Map<String, String> all = commands.hgetall(key);
            long now = System.currentTimeMillis();
            // 惰性清过期 + 找最低 seq
            long minSeq = Long.MAX_VALUE;
            String minField = null;
            for (Map.Entry<String, String> e : all.entrySet()) {
                Entry entry = decode(e.getValue());
                if (entry == null || entry.expireAtMillis() <= now) {
                    commands.hdel(key, e.getKey());
                    continue;
                }
                if (entry.seq() < minSeq) {
                    minSeq = entry.seq();
                    minField = e.getKey();
                }
            }
            if (all.size() >= maxEntriesPerBucket && minField != null) {
                commands.hdel(key, minField);
            }
            Entry fresh = new Entry(embedding.clone(),
                    payload == null ? "" : payload,
                    now + ttl.toMillis(),
                    nextSeq());
            commands.hset(key, entryId, RedisJson.write(fresh));
            commands.expire(key, Math.max(1, ttl.toSeconds()));
        } catch (RuntimeException e) {
            BuzhouMetricsHolder.metrics().counter("buzhou.semantic.redis.bypass", 1);
        }
    }

    /**
     * 桶内最近邻查询：cosine ≥ 阈值的最近条目命中（过期条目命中路径惰性清除）；
     * 无桶 / 无达标条目 / 后端不可达 = miss（旁路不抛——缓存 fail-open）。
     */
    public Optional<Hit> findNearest(String bucket, float[] queryEmbedding) {
        if (bucket == null || queryEmbedding == null || queryEmbedding.length == 0) {
            BuzhouMetricsHolder.metrics().counter("buzhou.semantic.redis.miss", 1);
            return Optional.empty();
        }
        String key = bucketKey(bucket);
        try {
            Map<String, String> all = commands.hgetall(key);
            long now = System.currentTimeMillis();
            String bestField = null;
            Entry best = null;
            double bestSim = threshold;
            for (Map.Entry<String, String> e : all.entrySet()) {
                Entry entry = decode(e.getValue());
                if (entry == null) {
                    continue;
                }
                if (entry.expireAtMillis() <= now) {
                    commands.hdel(key, e.getKey());
                    continue;
                }
                double sim = cosine(queryEmbedding, entry.emb());
                if (sim >= bestSim) {
                    bestSim = sim;
                    bestField = e.getKey();
                    best = entry;
                }
            }
            if (best == null) {
                BuzhouMetricsHolder.metrics().counter("buzhou.semantic.redis.miss", 1);
                return Optional.empty();
            }
            BuzhouMetricsHolder.metrics().counter("buzhou.semantic.redis.hit", 1);
            return Optional.of(new Hit(bestField, best.payload(), bestSim));
        } catch (RuntimeException e) {
            BuzhouMetricsHolder.metrics().counter("buzhou.semantic.redis.bypass", 1);
            return Optional.empty();
        }
    }

    /** 桶内条目数（诊断用；后端不可达 = -1）。 */
    public long size(String bucket) {
        try {
            return commands.hlen(bucketKey(bucket));
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /** 连接生命周期出口（宿主显式关闭；client.shutdown() 亦可覆盖）。 */
    public void close() {
        connection.close();
    }

    private String bucketKey(String bucket) {
        return keyPrefix + bucket;
    }

    private synchronized long nextSeq() {
        return ++seq;
    }

    private static Entry decode(String json) {
        try {
            Map<String, Object> map = RedisJson.readMap(json);
            if (map == null) {
                return null;
            }
            Object emb = map.get("emb");
            if (!(emb instanceof java.util.List<?> list)) {
                return null;
            }
            float[] vector = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                vector[i] = ((Number) list.get(i)).floatValue();
            }
            return new Entry(vector,
                    String.valueOf(map.getOrDefault("payload", "")),
                    ((Number) map.get("expireAtMillis")).longValue(),
                    ((Number) map.get("seq")).longValue());
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** cosine 相似度（零范数 → 0；维度不匹配 → 0——调用侧表现为不命中，与进程内版一致）。 */
    private static double cosine(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0.0;
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
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
