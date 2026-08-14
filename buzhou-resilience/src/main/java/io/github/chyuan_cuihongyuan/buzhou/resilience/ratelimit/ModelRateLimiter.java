package io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit;

import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.OverloadPolicy;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * 模型 RPM+TPM 双桶限流器（spec「背压 · 维度③ 模型 RPM+TPM 双桶」）。
 *
 * <p>按 modelName 分桶、单进程内存（{@link ConcurrentHashMap}，不引外部依赖）。
 * 令牌桶模型：RPM 桶 {@code capacity = rpm, refillRate = rpm/60 (tokens/s)}；
 * TPM 桶 {@code capacity = tpm, refillRate = tpm/60 (tokens/s)}。
 *
 * <ul>
 *   <li><b>RPM</b>：调用前预检 + 扣减（每次扣 1 token）；</li>
 *   <li><b>TPM</b>：调用后按实际 usage 记账（{@code usage.getTotalTokens()}）+ 下次调用前预检。
 *       <b>诚实边界</b>：TPM 是平均速率保护，不防单次尖峰越限——文档写明。</li>
 * </ul>
 *
 * <p>过载两档复用 {@link OverloadPolicy}：
 * <ul>
 *   <li>{@link OverloadPolicy#QUEUE QUEUE}（默认）——桶空时有界排队等待令牌补充，带超时；</li>
 *   <li>{@link OverloadPolicy#FAIL_FAST} ——桶空时立即拒绝。</li>
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
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * @param rpm          每分钟请求数上限（null = 不限）
     * @param tpm          每分钟 token 数上限（null = 不限）
     * @param queueTimeout QUEUE 档排队超时
     * @param policy       过载策略
     * @param emitter      事件发射器
     */
    public ModelRateLimiter(Integer rpm, Integer tpm, Duration queueTimeout,
                            OverloadPolicy policy, Consumer<SessionEvent> emitter) {
        this.rpm = rpm;
        this.tpm = tpm;
        this.queueTimeout = queueTimeout == null ? Duration.ofSeconds(30) : queueTimeout;
        this.policy = policy == null ? OverloadPolicy.QUEUE : policy;
        this.emitter = emitter == null ? event -> {} : emitter;
    }

    /** 是否启用了任何限流（rpm 或 tpm 任一非 null）。 */
    public boolean isEnabled() {
        return (rpm != null && rpm > 0) || (tpm != null && tpm > 0);
    }

    /**
     * 调用前预检 + 扣减 RPM + 预检 TPM。
     *
     * <p>先检 TPM（如有历史记账），再检+扣 RPM。两步都通过才放行。
     * 若 RPM/TPM 桶空，按过载策略排队或拒绝。
     *
     * @throws ModelRateLimitExceededException 限流拒绝（FAIL_FAST 立即 / QUEUE 超时）
     */
    public void acquireOrThrow(String modelName) {
        acquireOrThrow(modelName, null);
    }

    /**
     * 带事件通道的预检（impl-59：限流器进程级共享后，事件走当次调用会话的通道——
     * 构造期 emitter 仅作无调用侧 emitter 时的兜底）。
     */
    public void acquireOrThrow(String modelName, Consumer<SessionEvent> callSiteEmitter) {
        if (!isEnabled()) {
            return;
        }
        Consumer<SessionEvent> sink = callSiteEmitter != null ? callSiteEmitter : emitter;
        Bucket bucket = buckets.computeIfAbsent(modelName, k -> new Bucket(
                rpm != null && rpm > 0 ? rpm : 0,
                tpm != null && tpm > 0 ? tpm : 0));
        // TPM 预检（不扣减——实际记账在调用后）
        if (tpm != null && tpm > 0) {
            bucket.acquireTpm(modelName, policy, queueTimeout, sink);
        }
        // RPM 预检 + 扣减
        if (rpm != null && rpm > 0) {
            bucket.acquireRpm(modelName, policy, queueTimeout, sink);
        }
    }

    /**
     * 调用后按实际 usage 记账 TPM。
     *
     * @param modelName     模型名
     * @param totalTokens   本次调用的总 token 数（prompt + completion）；null/0 时记 0 并留痕
     */
    public void recordUsage(String modelName, Long totalTokens) {
        recordUsage(modelName, totalTokens, null);
    }

    /** 带事件通道的 TPM 记账（impl-59：同 {@link #acquireOrThrow(String, Consumer)} 口径）。 */
    public void recordUsage(String modelName, Long totalTokens, Consumer<SessionEvent> callSiteEmitter) {
        if (tpm == null || tpm <= 0) {
            return;
        }
        long tokens = totalTokens != null ? totalTokens : 0L;
        if (tokens == 0) {
            // provider 不返回 usage：记 0 + 留痕，不伪造估值
            (callSiteEmitter != null ? callSiteEmitter : emitter).accept(
                    new SessionEvent("backpressure.model-usage-missing",
                            Map.of("modelName", modelName), Instant.now()));
            return;
        }
        Bucket bucket = buckets.computeIfAbsent(modelName, k -> new Bucket(
                rpm != null && rpm > 0 ? rpm : 0,
                tpm != null && tpm > 0 ? tpm : 0));
        bucket.recordTpm(tokens);
    }

    // ---- 单模型令牌桶 ----

    private static final class Bucket {
        private final double rpmCapacity;
        private final double tpmCapacity;
        private final TokenBucket rpmBucket;
        private final TokenBucket tpmBucket;

        Bucket(int rpmCapacity, int tpmCapacity) {
            this.rpmCapacity = rpmCapacity;
            this.tpmCapacity = tpmCapacity;
            this.rpmBucket = rpmCapacity > 0
                    ? new TokenBucket(rpmCapacity, rpmCapacity / 60.0) : null;
            this.tpmBucket = tpmCapacity > 0
                    ? new TokenBucket(tpmCapacity, tpmCapacity / 60.0) : null;
        }

        void acquireRpm(String modelName, OverloadPolicy policy, Duration queueTimeout,
                        Consumer<SessionEvent> emitter) {
            acquire(rpmBucket, modelName, DIMENSION_RPM, 1.0, policy, queueTimeout, emitter);
        }

        void acquireTpm(String modelName, OverloadPolicy policy, Duration queueTimeout,
                        Consumer<SessionEvent> emitter) {
            if (tpmBucket == null) {
                return;
            }
            // TPM 预检：检查可用量 > 0，不扣减（实际扣减在 recordTpm）。
            // 诚实语义：TPM 是平均速率保护——若当前桶已空（前一次调用用尽预算），预检拒绝。
            if (tpmBucket.available() > 0) {
                return;
            }
            // 桶空：按策略排队等待令牌补充 / 拒绝（不扣减——等待 refill 到 > 0）
            if (policy == OverloadPolicy.FAIL_FAST) {
                emitRejected(emitter, modelName, DIMENSION_TPM, Duration.ZERO);
                throw new ModelRateLimitExceededException(modelName, DIMENSION_TPM, Duration.ZERO);
            }
            // QUEUE：等待令牌补充
            emitter.accept(new SessionEvent(EVENT_MODEL_THROTTLED,
                    Map.of("modelName", modelName, "dimension", DIMENSION_TPM),
                    Instant.now()));
            Instant start = Instant.now();
            while (tpmBucket.available() <= 0) {
                Duration remaining = queueTimeout.minus(Duration.between(start, Instant.now()));
                if (remaining.isZero() || remaining.isNegative()) {
                    emitRejected(emitter, modelName, DIMENSION_TPM, Duration.between(start, Instant.now()));
                    throw new ModelRateLimitExceededException(modelName, DIMENSION_TPM,
                            Duration.between(start, Instant.now()));
                }
                double waitSec = tpmBucket.timeUntilAvailable(1.0);
                long sleepMs = Math.min(remaining.toMillis(), Math.max(1, (long) (waitSec * 1000)));
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    emitRejected(emitter, modelName, DIMENSION_TPM, Duration.between(start, Instant.now()));
                    throw new ModelRateLimitExceededException(modelName, DIMENSION_TPM,
                            Duration.between(start, Instant.now()));
                }
            }
        }

        void recordTpm(long tokens) {
            if (tpmBucket != null) {
                tpmBucket.consume(tokens);
            }
        }

        private static void acquire(TokenBucket bucket, String modelName, String dimension,
                                    double amount, OverloadPolicy policy, Duration queueTimeout,
                                    Consumer<SessionEvent> emitter) {
            if (bucket == null) {
                return;
            }
            if (bucket.tryConsume(amount)) {
                return;
            }
            // 桶空——按策略处置
            if (policy == OverloadPolicy.FAIL_FAST) {
                emitRejected(emitter, modelName, dimension, Duration.ZERO);
                throw new ModelRateLimitExceededException(modelName, dimension, Duration.ZERO);
            }
            // QUEUE：有界排队等待令牌补充
            emitter.accept(new SessionEvent(EVENT_MODEL_THROTTLED,
                    Map.of("modelName", modelName, "dimension", dimension),
                    Instant.now()));
            Instant start = Instant.now();
            while (true) {
                double waitSec = bucket.timeUntilAvailable(amount);
                if (waitSec <= 0) {
                    if (bucket.tryConsume(amount)) {
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

    /**
     * 令牌桶：按时间补充令牌，消费时扣减。
     *
     * <p>线程安全：所有方法 synchronized。RPM/TPM 桶的并发量很低（每会话每轮一次模型调用），
     * synchronized 足够；高并发场景可换 StampedLock。
     */
    private static final class TokenBucket {
        private final double capacity;
        private final double refillRatePerSec;
        private double tokens;
        private long lastRefillNs;

        TokenBucket(double capacity, double refillRatePerSec) {
            this.capacity = capacity;
            this.refillRatePerSec = refillRatePerSec;
            this.tokens = capacity;
            this.lastRefillNs = System.nanoTime();
        }

        synchronized boolean tryConsume(double amount) {
            refill();
            if (amount <= 0) {
                return true;  // 0 消费总是成功（TPM 预检用）
            }
            if (tokens >= amount) {
                tokens -= amount;
                return true;
            }
            return false;
        }

        synchronized double available() {
            refill();
            return tokens;
        }

        synchronized double timeUntilAvailable(double amount) {
            refill();
            if (tokens >= amount) {
                return 0;
            }
            if (refillRatePerSec <= 0) {
                return Double.MAX_VALUE;
            }
            return (amount - tokens) / refillRatePerSec;
        }

        synchronized void consume(double amount) {
            // TPM 事后记账：扣减令牌（可能为负——诚实表达超限，下次预检拒绝）
            refill();
            tokens -= amount;
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsedSec = (now - lastRefillNs) / 1_000_000_000.0;
            tokens = Math.min(capacity, tokens + elapsedSec * refillRatePerSec);
            lastRefillNs = now;
        }
    }
}
