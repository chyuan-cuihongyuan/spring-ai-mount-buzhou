package io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.RateLimitBackend;

import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.OverloadPolicy;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 模型 RPM+TPM 双桶限流器（spec「背压 · 维度③」；spec 54 §A / T222 后端化改造）。
 *
 * <p><b>策略在本层</b>（过载两档：QUEUE 有界排队 / FAIL_FAST 立即拒 + 事件发射）；
 * <b>额度存取在后端</b>（{@link RateLimitBackend}）：默认
 * {@link InMemoryRateLimitBackend}（单进程令牌桶，行为零变化）；多实例部署可注入
 * Redis 固定窗后端（共享额度；整形特性差异见后端 javadoc，诚实入档）。
 *
 * <ul>
 *   <li><b>RPM</b>：调用前预检 + 扣减（每次扣 1）；</li>
 *   <li><b>TPM</b>：调用后按实际 usage 记账 + 下次调用前预检（平均速率保护，
 *       不防单次尖峰越限——诚实边界沿用）。</li>
 * </ul>
 *
 * <p>provider 不返回 usage 时 TPM 记 0 并留痕，不伪造估值。
 */
public final class ModelRateLimiter {

    /** 事件类型：模型调用被限流（排队等待中）。 */
    public static final String EVENT_MODEL_THROTTLED = "backpressure.model-throttled";
    /** 事件类型：模型调用被拒绝（超时 / fail-fast）。 */
    public static final String EVENT_MODEL_REJECTED = "backpressure.model-rejected";

    /** 桶维度常量。 */
    public static final String DIMENSION_RPM = "RPM";
    public static final String DIMENSION_TPM = "TPM";

    private final Integer rpm;
    private final Integer tpm;
    private final Duration queueTimeout;
    private final OverloadPolicy policy;
    private final Consumer<SessionEvent> emitter;
    private final RateLimitBackend backend;

    /**
     * @param rpm          每分钟请求数上限（null = 不限）
     * @param tpm          每分钟 token 数上限（null = 不限）
     * @param queueTimeout QUEUE 档排队超时
     * @param policy       过载策略
     * @param emitter      事件发射器
     */
    public ModelRateLimiter(Integer rpm, Integer tpm, Duration queueTimeout,
                            OverloadPolicy policy, Consumer<SessionEvent> emitter) {
        this(rpm, tpm, queueTimeout, policy, emitter, new InMemoryRateLimitBackend(rpm, tpm));
    }

    /** 后端注入构造（多实例共享额度路径；starter 按 store 形态装配）。 */
    public ModelRateLimiter(Integer rpm, Integer tpm, Duration queueTimeout,
                            OverloadPolicy policy, Consumer<SessionEvent> emitter,
                            RateLimitBackend backend) {
        this.rpm = rpm;
        this.tpm = tpm;
        this.queueTimeout = queueTimeout == null ? Duration.ofSeconds(30) : queueTimeout;
        this.policy = policy == null ? OverloadPolicy.QUEUE : policy;
        this.emitter = emitter == null ? event -> {} : emitter;
        this.backend = backend != null ? backend : new InMemoryRateLimitBackend(rpm, tpm);
    }

    /** 生效后端（观测面：kind = memory/redis）。 */
    public RateLimitBackend backend() {
        return backend;
    }

    /** 是否启用了任何限流（rpm 或 tpm 任一非 null）。 */
    public boolean isEnabled() {
        return (rpm != null && rpm > 0) || (tpm != null && tpm > 0);
    }

    /**
     * 调用前预检 + 扣减 RPM + 预检 TPM（两步都通过才放行；桶空按过载策略处置）。
     *
     * @throws ModelRateLimitExceededException 限流拒绝（FAIL_FAST 立即 / QUEUE 超时）
     */
    public void acquireOrThrow(String modelName) {
        acquireOrThrow(modelName, null);
    }

    /** 带事件通道的预检（impl-59：事件走当次调用会话的通道）。 */
    public void acquireOrThrow(String modelName, Consumer<SessionEvent> callSiteEmitter) {
        if (!isEnabled()) {
            return;
        }
        Consumer<SessionEvent> sink = callSiteEmitter != null ? callSiteEmitter : emitter;
        // TPM 预检（不扣减——实际记账在调用后；等待目标 = 1 个额度可用，沿用原语义）
        if (tpm != null && tpm > 0) {
            acquireDimension(backend, modelName, DIMENSION_TPM, 0, 1, sink);
        }
        // RPM 预检 + 扣减
        if (rpm != null && rpm > 0) {
            acquireDimension(backend, modelName, DIMENSION_RPM, 1, 1, sink);
        }
    }

    /** 调用后按实际 usage 记账 TPM（null/0 记 0 并留痕，不伪造估值）。 */
    public void recordUsage(String modelName, Long totalTokens) {
        recordUsage(modelName, totalTokens, null);
    }

    /** 带事件通道的 TPM 记账（同 acquireOrThrow 口径）。 */
    public void recordUsage(String modelName, Long totalTokens, Consumer<SessionEvent> callSiteEmitter) {
        if (tpm == null || tpm <= 0) {
            return;
        }
        long tokens = totalTokens != null ? totalTokens : 0L;
        if (tokens == 0) {
            (callSiteEmitter != null ? callSiteEmitter : emitter).accept(
                    new SessionEvent("backpressure.model-usage-missing",
                            Map.of("modelName", modelName), Instant.now()));
            return;
        }
        backend.consume(modelName, DIMENSION_TPM, tokens);
    }

    /** spec 49 §B / T177：模型桶剩余水位探针（0..1；未启用维度/无桶返回 1）。 */
    public double remainingRatio(String modelName, String dimension) {
        double capacity = backend.capacity(dimension);
        if (capacity <= 0) {
            return 1.0;
        }
        return Math.clamp(backend.available(modelName, dimension) / capacity, 0.0, 1.0);
    }

    /** 维度获取（amount=0 纯预检不扣；waitAmount=等待目标额度）；桶空按策略排队/拒绝。 */
    private void acquireDimension(RateLimitBackend backend, String modelName, String dimension,
            double amount, double waitAmount, Consumer<SessionEvent> emitter) {
        if (backend.tryAcquire(modelName, dimension, amount)) {
            return;
        }
        if (policy == OverloadPolicy.FAIL_FAST) {
            emitRejected(emitter, modelName, dimension, Duration.ZERO);
            throw new ModelRateLimitExceededException(modelName, dimension, Duration.ZERO);
        }
        // QUEUE：有界排队等待额度补充
        emitter.accept(new SessionEvent(EVENT_MODEL_THROTTLED,
                Map.of("modelName", modelName, "dimension", dimension),
                Instant.now()));
        Instant start = Instant.now();
        while (true) {
            double waitSec = backend.secondsUntilAvailable(modelName, dimension, waitAmount);
            if (waitSec <= 0) {
                if (backend.tryAcquire(modelName, dimension, amount)) {
                    return;
                }
                continue;
            }
            Duration remaining = queueTimeout.minus(Duration.between(start, Instant.now()));
            if (remaining.isZero() || remaining.isNegative()) {
                emitRejected(emitter, modelName, dimension, Duration.between(start, Instant.now()));
                throw new ModelRateLimitExceededException(modelName, dimension,
                        Duration.between(start, Instant.now()));
            }
            long sleepMs = Math.min(remaining.toMillis(), Math.max(1, (long) (waitSec * 1000)));
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                emitRejected(emitter, modelName, dimension, Duration.between(start, Instant.now()));
                throw new ModelRateLimitExceededException(modelName, dimension,
                        Duration.between(start, Instant.now()));
            }
        }
    }

    private static void emitRejected(Consumer<SessionEvent> emitter, String modelName,
                                     String dimension, Duration waited) {
        emitter.accept(new SessionEvent(EVENT_MODEL_REJECTED,
                Map.of("modelName", modelName, "dimension", dimension,
                        "waitedMs", waited.toMillis()),
                Instant.now()));
    }
}
