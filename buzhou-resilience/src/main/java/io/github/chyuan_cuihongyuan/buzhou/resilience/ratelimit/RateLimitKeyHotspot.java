package io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 限流键热点读数（spec 837 / T1177，Envoy per-connection rate limit 键域
 * 观测思想）：限流键（模型×维度组合——虚拟密钥接入后即 key 级）的申请次数
 * 聚合——「限流额度被谁消耗/有没有热点键独占」结构化（TagCardinalityGuard
 * 是指标标签域——限流键域互补）。
 *
 * <p>纯读数：键封顶 {@value #MAX_KEYS}（超限并入 {@link #OVERFLOW} 桶——
 * 跨域口径一致）；record(null/空白/负额) 忽略；top(n) 按申请额降序。
 * 喂点=ModelRateLimiter 策略层装配侧（不改 backend SPI）。
 */
public final class RateLimitKeyHotspot {

    /** 键数封顶。 */
    public static final int MAX_KEYS = 128;
    /** 溢出桶名。 */
    public static final String OVERFLOW = "__overflow__";

    /** 单键行。 */
    public record KeyDemand(String key, long requests, double amountSum, long lastSeenMillis) {
    }

    private static final class Counter {
        final AtomicLong requests = new AtomicLong();
        final AtomicLong amountMillis = new AtomicLong(); // amount×1000 累计（避免 double CAS）
        volatile long lastSeen;
    }

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final AtomicLong totalRequests = new AtomicLong();

    /** 记录一次限流申请（key=model|dimension 组合由调用方拼；amount 语义由调用方定）。 */
    public void record(String key, double amount, long atMillis) {
        if (key == null || key.isBlank() || amount < 0) {
            return;
        }
        totalRequests.incrementAndGet();
        String bucket = key;
        if (!counters.containsKey(bucket) && counters.size() >= MAX_KEYS) {
            bucket = OVERFLOW;
        }
        Counter counter = counters.computeIfAbsent(bucket, k -> new Counter());
        counter.requests.incrementAndGet();
        counter.amountMillis.addAndGet((long) (amount * 1000));
        counter.lastSeen = Math.max(counter.lastSeen, atMillis);
    }

    /** 申请额最高的前 n 键（requests 降序，典序破平）。 */
    public List<KeyDemand> top(int n) {
        List<KeyDemand> all = new ArrayList<>();
        for (Map.Entry<String, Counter> e : counters.entrySet()) {
            Counter c = e.getValue();
            all.add(new KeyDemand(e.getKey(), c.requests.get(),
                    c.amountMillis.get() / 1000.0, c.lastSeen));
        }
        all.sort(Comparator.comparingLong(KeyDemand::requests).reversed()
                .thenComparing(KeyDemand::key));
        if (n <= 0) {
            return List.of();
        }
        return List.copyOf(all.subList(0, Math.min(n, all.size())));
    }

    public long totalRequests() {
        return totalRequests.get();
    }

    public int distinctKeys() {
        return counters.size();
    }
}
