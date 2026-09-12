package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 跨轮 TTL 工具缓存装饰器（spec 183 / T555，HTTP max-age 借鉴）：key = 工具名 +
 * argsHash；TTL 窗内复读直接回缓存值（引用一致）；过期惰性重执行；LRU 封顶；
 * <b>失败不缓存</b>（可重试信号——与轮内 memo 同口径）。契约：只包时效钝感的
 * 只读工具。定义透传（装饰器家族同款）。
 *
 * <p>spec 701 / T953（nginx {@code proxy_cache_use_stale} / RFC 5861 借鉴）：
 * opt-in SWR——swrGrace 窗内过期命中<b>同步回 stale 值</b>（调用者零等待）+
 * 虚拟线程后台单飞刷新（in-flight 集合去重：nginx proxy_cache_lock 同款）；
 * 刷新失败保留旧 stale 值（{@code use_stale error} 语义）；grace 窗外硬过期。
 * 默认 grace=0 现行为逐字节不变。
 */
public final class TtlCachingToolCallback implements ToolCallback {

    private static final Logger LOG = LoggerFactory.getLogger(TtlCachingToolCallback.class);

    private static final String SWR_THREAD_PREFIX = "buzhou-ttl-swr-";

    /** 观测计数（hit/miss/evicted）。 */
    public record Stats(long hits, long misses, long evictions) {
    }

    private record Cached(String value, long expireAtMillis) {
    }

    private final ToolCallback delegate;
    private final Duration maxAge;
    private final Clock clock;
    private final Duration swrGrace;
    private final Set<String> refreshing = ConcurrentHashMap.newKeySet();
    private final LinkedHashMap<String, Cached> cache;
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();
    private final AtomicLong staleServed = new AtomicLong();
    private final AtomicLong refreshFailures = new AtomicLong();

    private TtlCachingToolCallback(ToolCallback delegate, Duration maxAge, int maxEntries,
                                   Clock clock, Duration swrGrace) {
        this.delegate = delegate;
        this.maxAge = maxAge;
        this.clock = clock;
        this.swrGrace = swrGrace;
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Cached> eldest) {
                boolean evict = size() > maxEntries;
                if (evict) {
                    evictions.incrementAndGet();
                }
                return evict;
            }
        };
    }

    public static TtlCachingToolCallback wrap(ToolCallback delegate, Duration maxAge,
                                              int maxEntries) {
        return wrap(delegate, maxAge, maxEntries, Clock.systemUTC());
    }

    public static TtlCachingToolCallback wrap(ToolCallback delegate, Duration maxAge,
                                              int maxEntries, Clock clock) {
        return wrap(delegate, maxAge, maxEntries, clock, Duration.ZERO);
    }

    /**
     * spec 701：SWR 变体——swrGrace 正时长开启「过期后 grace 窗内先回 stale +
     * 后台刷新」；零时长 = 关（现行为）。
     */
    public static TtlCachingToolCallback wrap(ToolCallback delegate, Duration maxAge,
                                              int maxEntries, Clock clock, Duration swrGrace) {
        if (delegate == null || maxAge == null || maxAge.isZero() || maxAge.isNegative()
                || maxEntries < 1 || clock == null
                || swrGrace == null || swrGrace.isNegative()) {
            throw new IllegalArgumentException(
                    "delegate 非空、maxAge 正、maxEntries>=1、clock 非空、swrGrace 非负");
        }
        return new TtlCachingToolCallback(delegate, maxAge, maxEntries, clock, swrGrace);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return withCache(key(toolInput), () -> delegate.call(toolInput));
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return withCache(key(toolInput), () -> delegate.call(toolInput, toolContext));
    }

    private String withCache(String key, java.util.function.Supplier<String> execution) {
        synchronized (cache) {
            Cached cached = cache.get(key);
            if (cached != null) {
                long now = clock.millis();
                if (now < cached.expireAtMillis()) {
                    hits.incrementAndGet();
                    return cached.value();
                }
                if (!swrGrace.isZero() && now < cached.expireAtMillis() + swrGrace.toMillis()) {
                    // spec 701：SWR grace 窗——同步回 stale（调用者零等待）+ 后台单飞刷新
                    staleServed.incrementAndGet();
                    if (refreshing.add(key)) {
                        Thread.ofVirtual().name(SWR_THREAD_PREFIX, 0)
                                .start(() -> refresh(key, execution));
                    }
                    return cached.value();
                }
                cache.remove(key); // 硬过期（grace 窗外 / 默认关）：过期惰性清
            }
        }
        misses.incrementAndGet();
        String value = execution.get(); // 失败（异常）不入表——锁外执行防长持锁
        synchronized (cache) {
            cache.put(key, new Cached(value, clock.millis() + maxAge.toMillis()));
        }
        return value;
    }

    /** spec 701：后台单飞刷新——成功落新值；失败保旧 stale（use_stale error 语义）。 */
    private void refresh(String key, java.util.function.Supplier<String> execution) {
        try {
            String value = execution.get(); // 失败（异常）不入表——与主路径同口径
            synchronized (cache) {
                cache.put(key, new Cached(value, clock.millis() + maxAge.toMillis()));
            }
        } catch (RuntimeException e) {
            refreshFailures.incrementAndGet();
            LOG.warn("SWR 后台刷新失败，保留 stale 值 key={}: {}", key, e.getMessage());
        } finally {
            refreshing.remove(key);
        }
    }

    private String key(String toolInput) {
        return delegate.getToolDefinition().name() + ":" + ToolCallLogEntry.argsHash(toolInput);
    }

    /** 观测计数。 */
    public Stats stats() {
        return new Stats(hits.get(), misses.get(), evictions.get());
    }

    /** spec 701：SWR stale 回程计数（grace 窗内过期命中次数）。 */
    public long staleServedCount() {
        return staleServed.get();
    }

    /** spec 701：后台刷新失败计数（失败保留旧 stale 值）。 */
    public long refreshFailureCount() {
        return refreshFailures.get();
    }

    /** 当前缓存条数（观测/测试）。 */
    public int size() {
        synchronized (cache) {
            return cache.size();
        }
    }
}
