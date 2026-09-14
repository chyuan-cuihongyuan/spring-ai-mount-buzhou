package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 工具失败负缓存装饰器（spec 1616 / T2383，DNS negative caching / NXDOMAIN 短 TTL
 * 思想）：同 key（工具名 + argsHash）失败结果短 TTL 记忆——窗内复读直接回上次
 * 错误文本不再真调（模型反复撞同一失败是真实负载：参数错误/下游故障期的重试风暴）。
 * TTL 到期即放行真调（故障恢复窗口 = TTL 本身——DNS 负缓存同款短窗纪律，故 TTL
 * 须短，默认 30s）。与 TTL 成功缓存（spec 183）正交：<b>那只缓存成功、这只只缓存
 * 失败</b>。
 *
 * <p>失败判定：结构化错误标记（{@link ToolFeedbackType#isErrorFeedback}——执行
 * 失败与校验失败均计）或抛出的 RuntimeException。契约：TTL 短（默认 30s——暂态
 * 故障快速恢复，DNS 负缓存同款短窗纪律）；LRU 封顶；定义透传（装饰器族同款）。
 * @since 1.0.0
 */
public final class NegativeCachingToolCallback implements ToolCallback {

    /** 观测计数（negativeHits=negTtl 窗内拦截数；stored=新缓存失败）。 */
    public record Stats(long negativeHits, long stored, long evictions) {
    }

    private record CachedFailure(String errorText, long expireAtMillis) {
    }

    private final ToolCallback delegate;
    private final Duration negTtl;
    private final Clock clock;
    private final LinkedHashMap<String, CachedFailure> failures;
    private final AtomicLong negativeHits = new AtomicLong();
    private final AtomicLong stored = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();

    private NegativeCachingToolCallback(ToolCallback delegate, Duration negTtl,
            int maxEntries, Clock clock) {
        this.delegate = delegate;
        this.negTtl = negTtl;
        this.clock = clock;
        this.failures = new LinkedHashMap<>(16, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CachedFailure> eldest) {
                boolean evict = size() > maxEntries;
                if (evict) {
                    evictions.incrementAndGet();
                }
                return evict;
            }
        };
    }

    /** 默认 30s 负 TTL / 256 条封顶。 */
    public static NegativeCachingToolCallback wrap(ToolCallback delegate, Duration negTtl) {
        return new NegativeCachingToolCallback(delegate, negTtl, 256, Clock.systemUTC());
    }

    /** 全参（maxEntries 封顶；测试时钟注入）。 */
    public static NegativeCachingToolCallback wrap(ToolCallback delegate, Duration negTtl,
            int maxEntries, Clock clock) {
        return new NegativeCachingToolCallback(delegate, negTtl, maxEntries, clock);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        String key = delegate.getToolDefinition().name() + "#" + argsHash(toolInput);
        synchronized (failures) {
            CachedFailure cached = failures.get(key);
            if (cached != null) {
                if (clock.instant().toEpochMilli() < cached.expireAtMillis()) {
                    negativeHits.incrementAndGet();
                    return cached.errorText();
                }
                failures.remove(key); // 过期——放行真调（暂态故障可能已恢复）
            }
        }
        String result;
        try {
            result = toolContext == null
                    ? delegate.call(toolInput) : delegate.call(toolInput, toolContext);
        } catch (RuntimeException e) {
            store(key, "工具执行异常：" + e.getMessage());
            throw e;
        }
        if (ToolFeedbackType.isErrorFeedback(result)) {
            store(key, result);
        }
        // 成功不缓存（成功归 spec 183 TTL memo 族）；TTL 到期自然放行——恢复窗口 = TTL
        return result;
    }

    private void store(String key, String errorText) {
        synchronized (failures) {
            failures.put(key, new CachedFailure(errorText,
                    clock.instant().toEpochMilli() + negTtl.toMillis()));
        }
        stored.incrementAndGet();
    }

    private static String argsHash(String toolInput) {
        return Integer.toHexString(String.valueOf(toolInput).hashCode());
    }

    /** 观测快照。 */
    public Stats stats() {
        synchronized (failures) {
            return new Stats(negativeHits.get(), stored.get(), evictions.get());
        }
    }

    /** 当前缓存失败条数（观测/测试）。 */
    public int size() {
        synchronized (failures) {
            return failures.size();
        }
    }
}
