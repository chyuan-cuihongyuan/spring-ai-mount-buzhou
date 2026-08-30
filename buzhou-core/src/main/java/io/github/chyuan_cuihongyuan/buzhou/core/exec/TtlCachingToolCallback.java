package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLogEntry;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 跨轮 TTL 工具缓存装饰器（spec 183 / T555，HTTP max-age 借鉴）：key = 工具名 +
 * argsHash；TTL 窗内复读直接回缓存值（引用一致）；过期惰性重执行；LRU 封顶；
 * <b>失败不缓存</b>（可重试信号——与轮内 memo 同口径）。契约：只包时效钝感的
 * 只读工具。定义透传（装饰器家族同款）。
 */
public final class TtlCachingToolCallback implements ToolCallback {

    /** 观测计数（hit/miss/evicted）。 */
    public record Stats(long hits, long misses, long evictions) {
    }

    private record Cached(String value, long expireAtMillis) {
    }

    private final ToolCallback delegate;
    private final Duration maxAge;
    private final Clock clock;
    private final LinkedHashMap<String, Cached> cache;
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();

    private TtlCachingToolCallback(ToolCallback delegate, Duration maxAge, int maxEntries,
                                   Clock clock) {
        this.delegate = delegate;
        this.maxAge = maxAge;
        this.clock = clock;
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
        if (delegate == null || maxAge == null || maxAge.isZero() || maxAge.isNegative()
                || maxEntries < 1 || clock == null) {
            throw new IllegalArgumentException("delegate 非空、maxAge 正、maxEntries>=1、clock 非空");
        }
        return new TtlCachingToolCallback(delegate, maxAge, maxEntries, clock);
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
            if (cached != null && clock.millis() < cached.expireAtMillis()) {
                hits.incrementAndGet();
                return cached.value();
            }
            if (cached != null) {
                cache.remove(key); // 过期惰性清
            }
        }
        misses.incrementAndGet();
        String value = execution.get(); // 失败（异常）不入表——锁外执行防长持锁
        synchronized (cache) {
            cache.put(key, new Cached(value, clock.millis() + maxAge.toMillis()));
        }
        return value;
    }

    private String key(String toolInput) {
        return delegate.getToolDefinition().name() + ":" + ToolCallLogEntry.argsHash(toolInput);
    }

    /** 观测计数。 */
    public Stats stats() {
        return new Stats(hits.get(), misses.get(), evictions.get());
    }

    /** 当前缓存条数（观测/测试）。 */
    public int size() {
        synchronized (cache) {
            return cache.size();
        }
    }
}
