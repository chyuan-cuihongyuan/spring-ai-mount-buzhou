package io.github.chyuan_cuihongyuan.buzhou.resilience.concurrency;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 模型并发舱（spec 426 / T743，Resilience4j SemaphoreBulkhead / Uber
 * concurrency-limits 借鉴——供应商并发配额分层）：per-model 在飞并发
 * 上限。未配置模型 = NOOP 零开销（AgentBulkhead 先例）；超限
 * {@code acquire-timeout}（默认 0 = fail-fast）内取不到抛
 * {@link BuzhouException}（QUOTA_EXCEEDED——AgentBulkhead 同词汇，
 * NON_RETRYABLE 信号质量优先）+ 计数（model tag bounded）。
 *
 * <p>多实例诚实边界：每实例独立并发额度（无共享后端——限流族同注记，
 * runbook §6）。在飞语义 = 逻辑模型调用占用一次许可、重试期间持续持有
 * （advisor 链序保证——见 {@link ModelConcurrencyAdvisor}）。
 */
public final class ModelConcurrencyLimiter {

    private static final System.Logger LOGGER = System.getLogger(ModelConcurrencyLimiter.class.getName());

    private volatile Map<String, Integer> limits;
    private final Duration acquireTimeout;
    private final Map<String, ResizableSemaphore> semaphores = new ConcurrentHashMap<>();

    /**
     * @param limits         模型名 → 并发上限（&lt;1 视为 NOOP 不限）
     * @param acquireTimeout 取许可等待（null/0 = fail-fast 立即拒绝）
     */
    public ModelConcurrencyLimiter(Map<String, Integer> limits, Duration acquireTimeout) {
        this.limits = limits == null ? Map.of() : Map.copyOf(limits);
        this.acquireTimeout = acquireTimeout == null ? Duration.ZERO : acquireTimeout;
    }

    /** 取在飞许可（未配置模型 NOOP；超限抛 QUOTA_EXCEEDED）。 */
    public void acquireOrThrow(String model) {
        Integer limit = limits.get(model);
        if (limit == null || limit < 1) {
            return; // NOOP——未配置即不限（零开销）
        }
        Semaphore semaphore = semaphores.computeIfAbsent(model, k -> new ResizableSemaphore(limit));
        boolean acquired;
        try {
            acquired = acquireTimeout.isZero()
                    ? semaphore.tryAcquire()
                    : semaphore.tryAcquire(acquireTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BuzhouException(ErrorCode.QUOTA_EXCEEDED,
                    "模型并发舱等待被中断（model=" + model + "）");
        }
        if (!acquired) {
            BuzhouMetricsHolder.metrics().counter(
                    "buzhou.resilience.concurrency-rejected", "model", bounded(model));
            throw new BuzhouException(ErrorCode.QUOTA_EXCEEDED,
                    "模型并发舱已满（model=" + model + "，上限 " + limit
                            + "，在飞 " + inFlightOf(semaphore, limit) + "）——稍后重试或调参");
        }
    }

    /** 归还在飞许可（未配置模型 NOOP）。 */
    public void release(String model) {
        Semaphore semaphore = semaphores.get(model);
        if (semaphore != null) {
            semaphore.release();
        }
    }

    /**
     * spec 429 / T749：热调整上限表（320 AgentBulkhead.resize 同语义）：
     * 扩容 grow / 缩容 shrink（reducePermits）——<b>在飞不受扰</b>，瞬时可
     * 超新限，释放到限内才放新请求，不抢占。新模型建舱；移除键摘舱（在飞
     * 释放到已摘对象无害；新 acquire=NOOP）。逐键 WARN diff 留痕（417 价目
     * 热载同款审计面）。
     */
    public synchronized void resize(Map<String, Integer> newLimits) {
        Map<String, Integer> target = newLimits == null ? Map.of() : Map.copyOf(newLimits);
        Map<String, Integer> old = limits;
        for (Map.Entry<String, Integer> entry : target.entrySet()) {
            int limit = Math.max(1, entry.getValue());
            Integer previous = old.get(entry.getKey());
            ResizableSemaphore existing = semaphores.get(entry.getKey());
            if (previous == null) {
                semaphores.putIfAbsent(entry.getKey(), new ResizableSemaphore(limit));
                LOGGER.log(System.Logger.Level.WARNING,
                        "模型并发舱建舱：model=" + entry.getKey() + " 上限 " + limit);
                continue;
            }
            if (existing == null) {
                continue; // 旧限已声明但从未取过（无舱）——limits 换表即生效
            }
            int diff = limit - previous;
            if (diff > 0) {
                existing.grow(diff);
            } else if (diff < 0) {
                existing.shrink(-diff);
            }
            if (diff != 0) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "模型并发舱调容：model=" + entry.getKey() + " " + previous + " → " + limit);
            }
        }
        for (String removed : old.keySet()) {
            if (!target.containsKey(removed)) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "模型并发舱摘舱：model=" + removed + "（在飞释放无害，新 acquire=NOOP）");
            }
        }
        semaphores.keySet().retainAll(target.keySet());
        limits = target;
        BuzhouMetricsHolder.metrics().counter("buzhou.resilience.concurrency-resized");
    }

    /** 各配置模型当前在飞数快照（观测面；键序=配置序）。 */
    public Map<String, Integer> inFlight() {
        Map<String, Integer> snapshot = new LinkedHashMap<>();
        limits.forEach((model, limit) -> {
            if (limit >= 1) {
                Semaphore semaphore = semaphores.get(model);
                snapshot.put(model, semaphore == null ? 0 : inFlightOf(semaphore, limit));
            }
        });
        return snapshot;
    }

    private static int inFlightOf(Semaphore semaphore, int limit) {
        return limit - semaphore.availablePermits();
    }

    private static String bounded(String model) {
        return io.github.chyuan_cuihongyuan.buzhou.resilience.MetricTags.bound(model);
    }

    /** 可调信号量（Semaphore.reducePermits 是 protected——子类公开化，resilience4j 同法）。 */
    static final class ResizableSemaphore extends Semaphore {

        ResizableSemaphore(int permits) {
            super(permits);
        }

        void grow(int permits) {
            release(permits);
        }

        /** 缩 permit（在飞超新限瞬时共存，释放自然收敛）。 */
        void shrink(int permits) {
            reducePermits(permits);
        }
    }
}
