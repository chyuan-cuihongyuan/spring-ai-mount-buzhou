package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * webhook 投递限速令牌桶（spec 718 / T987，envoy local rate limit 借鉴）：
 * {@code capacity}（burst）+ {@code refillPerSecond}（持续速率）——
 * {@link #tryAcquire()} 取令牌，取不到 = 本轮 defer（记录留 outbox 原状，
 * 下一 tick 令牌回填自然放行——tick 粒度即节流粒度）。
 *
 * <p><b>单 sink 语义</b>（OutboxRecord 无 URL——per-URL 桶随多 sink fanout
 * 留位）；时钟 {@link LongSupplier} 注入（毫秒，测试可推进）。进程内限速
 * （多实例全局限速归共享后端既有面）。令牌懒惰回填：取用时按流逝时间补给。
 */
public final class WebhookRateLimiter {

    private final double capacity;
    private final double refillPerSecond;
    private final LongSupplier clockMillis;
    private final AtomicLong deferred = new AtomicLong();

    private double tokens;
    private long lastRefillMillis;

    /** @param capacity 桶容量（burst，≥1）；@param refillPerSecond 持续速率（>0） */
    public WebhookRateLimiter(double capacity, double refillPerSecond, LongSupplier clockMillis) {
        if (capacity < 1 || refillPerSecond <= 0 || clockMillis == null) {
            throw new IllegalArgumentException("capacity>=1、refillPerSecond>0、clock 非空");
        }
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.clockMillis = clockMillis;
        this.tokens = capacity; // 满桶起步——首波 burst 即 capacity
        this.lastRefillMillis = clockMillis.getAsLong();
    }

    /** 同步取令牌（投递循环单线程调用——synchronized 简单正确）。 */
    public synchronized boolean tryAcquire() {
        refill();
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        deferred.incrementAndGet();
        return false;
    }

    private void refill() {
        long now = clockMillis.getAsLong();
        long elapsed = Math.max(0, now - lastRefillMillis);
        if (elapsed > 0) {
            tokens = Math.min(capacity, tokens + elapsed * refillPerSecond / 1000.0);
            lastRefillMillis = now;
        }
    }

    /** 累计 defer 次数（观测面——非零增长 = 下游消费速率跟不上事件速率）。 */
    public long deferredCount() {
        return deferred.get();
    }

    /**
     * impl-673 / spec 920：余量快照（synchronized 同锁强一致）——「令牌还剩多少/
     * 桶多大/流速多少」读面，区分「限流配置过低（余量常态贴 0）」与「突发超预期
     * （余量骤降后回填）」。tokens 为 refill 时点修正后的实时余量（与 acquire
     * 判定同语义）。纯读面，tryAcquire/deferredCount 行为零变化。
     */
    public record Snapshot(double tokens, double capacity, double refillPerSecond,
                           long deferred) {
    }

    public synchronized Snapshot snapshot() {
        refill();
        return new Snapshot(tokens, capacity, refillPerSecond, deferred.get());
    }
}
