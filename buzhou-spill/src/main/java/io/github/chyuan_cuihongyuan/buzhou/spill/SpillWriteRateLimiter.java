package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * spill 写入字节率限速器（spec 1619 / T2389，RocksDB rate limiter 思想）：
* 令牌桶按 bytes/秒 补充——写盘前 acquire(bytes) 阻节流（背压传导给上游，
 * 防溢出高峰打满磁盘带宽拖垮同机事务日志/页缓存）。令牌桶容量 = burstBytes
 * （短突发容忍）；等待超 {@code maxWaitMillis} 放行并计数（软限速——限速器
 * 自身故障不放大成 spill 失败，degraded 计数可观测）。
 *
 * <p>ReentrantLock + Condition（虚拟线程 unmount 不 pin——spec 1606 系纪律）；
 * 0 = 关（acquire 恒 true 零开销——默认零行为）。
 */
public final class SpillWriteRateLimiter {

    private final long bytesPerSecond;
    private final long burstBytes;
    private final long maxWaitMillis;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition tokensAvailable = lock.newCondition();
    private long availableTokens;
    private long lastRefillNanos;
    private final AtomicLong throttledWaits = new AtomicLong();
    private final AtomicLong degradedBypasses = new AtomicLong();

    public SpillWriteRateLimiter(long bytesPerSecond, long burstBytes, long maxWaitMillis) {
        if (bytesPerSecond < 0 || burstBytes < 0 || maxWaitMillis < 0) {
            throw new IllegalArgumentException(
                    "限速配置非法（bytesPerSecond/burstBytes/maxWaitMillis 均 >= 0）");
        }
        this.bytesPerSecond = bytesPerSecond;
        this.burstBytes = burstBytes;
        this.maxWaitMillis = maxWaitMillis;
        this.availableTokens = burstBytes;
        this.lastRefillNanos = System.nanoTime();
    }

    /** 是否启用（bytesPerSecond ≤ 0 = 关）。 */
    public boolean enabled() {
        return bytesPerSecond > 0;
    }

    /**
     * 节流获取（等待至令牌可得；超 maxWait 放行并 degraded 计数——软限速语义）。
     * 关闭态恒立即返回 true。
     */
    public boolean acquire(long bytes) throws InterruptedException {
        if (!enabled() || bytes <= 0) {
            return true;
        }
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(maxWaitMillis);
        lock.lockInterruptibly();
        try {
            while (true) {
                refill();
                if (availableTokens >= bytes) {
                    availableTokens -= bytes;
                    return true;
                }
                if (System.nanoTime() >= deadline) {
                    degradedBypasses.incrementAndGet(); // 软限速：超时放行（可观测）
                    return true;
                }
                throttledWaits.incrementAndGet();
                long waitNanos = Math.min(
                        TimeUnit.MILLISECONDS.toNanos(20),
                        deadline - System.nanoTime());
                if (waitNanos <= 0) {
                    continue;
                }
                tokensAvailable.await(waitNanos, TimeUnit.NANOSECONDS);
            }
        } finally {
            lock.unlock();
        }
    }

    /** 惰性补令牌（锁内调用）。 */
    private void refill() {
        long now = System.nanoTime();
        long elapsed = now - lastRefillNanos;
        if (elapsed <= 0) {
            return;
        }
        long refill = (long) ((double) bytesPerSecond * elapsed / 1_000_000_000L);
        if (refill > 0) {
            availableTokens = Math.min(burstBytes, availableTokens + refill);
            lastRefillNanos = now;
            tokensAvailable.signalAll();
        }
    }

    /** 节流等待发生的累计（观测面——每轮等待循环 +1）。 */
    public long throttledWaits() {
        return throttledWaits.get();
    }

    /** 超时放行累计（观测面——非零持续增长 = 限速配置跟不上实际写压）。 */
    public long degradedBypasses() {
        return degradedBypasses.get();
    }

    /** 当前可用令牌（观测/测试）。 */
    public long availableTokens() {
        lock.lock();
        try {
            refill();
            return availableTokens;
        } finally {
            lock.unlock();
        }
    }
}
