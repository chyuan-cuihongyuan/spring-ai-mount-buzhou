package io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.RateLimitBackend;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存令牌桶后端（spec 54 §A / T222）：原 {@code ModelRateLimiter.TokenBucket} 逻辑平移
 * （synchronized 桶 + 时间补充；refillRate = capacity/60）；默认后端——单进程行为零变化。
 */
public final class InMemoryRateLimitBackend implements RateLimitBackend {

    private final double rpmCapacity;
    private final double tpmCapacity;
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public InMemoryRateLimitBackend(Integer rpm, Integer tpm) {
        this.rpmCapacity = rpm != null && rpm > 0 ? rpm : 0;
        this.tpmCapacity = tpm != null && tpm > 0 ? tpm : 0;
    }

    @Override
    public boolean tryAcquire(String modelName, String dimension, double amount) {
        return bucketOf(modelName, dimension).tryConsume(amount);
    }

    @Override
    public void consume(String modelName, String dimension, double amount) {
        bucketOf(modelName, dimension).consume(amount);
    }

    @Override
    public double available(String modelName, String dimension) {
        return bucketOf(modelName, dimension).available();
    }

    @Override
    public double capacity(String dimension) {
        return switch (dimension) {
            case ModelRateLimiter.DIMENSION_RPM -> rpmCapacity;
            case ModelRateLimiter.DIMENSION_TPM -> tpmCapacity;
            default -> 0;
        };
    }

    @Override
    public double secondsUntilAvailable(String modelName, String dimension, double amount) {
        return bucketOf(modelName, dimension).timeUntilAvailable(amount);
    }

    @Override
    public String kind() {
        return "memory";
    }

    /** 桶键 = 模型:维度（原 ModelRateLimiter 按 modelName 分桶语义平移）。 */
    private TokenBucket bucketOf(String modelName, String dimension) {
        return buckets.computeIfAbsent(modelName + ":" + dimension, k -> new TokenBucket(
                capacity(dimension), capacity(dimension) / 60.0));
    }

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
                return tokens > 0; // 预检语义（TPM 预检沿用原实现：桶空拒绝，不扣减）
            }
            if (tokens >= amount) {
                tokens -= amount;
                return true;
            }
            return false;
        }

        synchronized double available() {
            refill();
            return Math.max(0, tokens);
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
