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

    private final Map<String, Integer> limits;
    private final Duration acquireTimeout;
    private final Map<String, Semaphore> semaphores = new ConcurrentHashMap<>();

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
        Semaphore semaphore = semaphores.computeIfAbsent(model, k -> new Semaphore(limit));
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
}
