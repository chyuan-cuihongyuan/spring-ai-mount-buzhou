package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 最小 RTT 滑窗滤波器（spec 2015 / T3131 / impl 1566）——TCP BBR
 * min-RTT 思想：带宽估计的基线是窗内最小往返时延（非均值——均值被
 * 队列膨胀污染，最小值最接近真传播时延）。窗（默认 10 分钟）滑出后
 * 旧最小惰性失效重算——网络改善能被追认；lastFreshMinAt 陈旧度读数
 * 显形「多久没见过新最小」（基线可信度）。
 *
 * <p>synchronized 小临界区；时间由调用方传入（确定性可回放）。
 */
public final class MinRttTracker {

    /** 默认窗口（毫秒）——BBR min-RTT 窗 10 分钟惯例。 */
    public static final long DEFAULT_WINDOW_MILLIS = 10L * 60 * 1000;

    private record Sample(long rttMillis, long atMillis) {
    }

    private final long windowMillis;
    private final Deque<Sample> samples = new ArrayDeque<>();
    private long lastFreshMinAt = -1;

    /** 契约：windowMillis &gt; 0（fail-fast）。 */
    public MinRttTracker(long windowMillis) {
        if (windowMillis <= 0) {
            throw new IllegalArgumentException("windowMillis 须 > 0：" + windowMillis);
        }
        this.windowMillis = windowMillis;
    }

    public MinRttTracker() {
        this(DEFAULT_WINDOW_MILLIS);
    }

    /** 观测一次 RTT（≥0；窗内入样，过期样惰性清除，见新最小记新鲜时刻）。 */
    public synchronized void observe(long rttMillis, long nowMillis) {
        if (rttMillis < 0) {
            throw new IllegalArgumentException("rttMillis 须 ≥ 0：" + rttMillis);
        }
        if (nowMillis < 0) {
            throw new IllegalArgumentException("nowMillis 须 ≥ 0：" + nowMillis);
        }
        evictExpired(nowMillis);
        long beforeMin = samples.isEmpty() ? Long.MAX_VALUE : currentMin();
        samples.addLast(new Sample(rttMillis, nowMillis));
        if (rttMillis < beforeMin) {
            lastFreshMinAt = nowMillis; // 新最小（首个样本 beforeMin=MAX 必刷新）
        }
    }

    /** 窗内最小 RTT（毫秒）；空窗 0——调用方以 {@link #sampleCount()} 判有效性。 */
    public synchronized long minRtt(long nowMillis) {
        evictExpired(nowMillis);
        if (samples.isEmpty()) {
            return 0L;
        }
        return currentMin();
    }

    /** 最近一次刷新最小的观测时刻（-1 = 从未见过）——基线陈旧度。 */
    public synchronized long lastFreshMinAt() {
        return lastFreshMinAt;
    }

    /** 窗内样本数（有效性判据 + 采样密度对账）。 */
    public synchronized int sampleCount() {
        return samples.size();
    }

    private long currentMin() {
        long min = Long.MAX_VALUE;
        for (Sample s : samples) {
            min = Math.min(min, s.rttMillis());
        }
        return min;
    }

    private void evictExpired(long nowMillis) {
        while (!samples.isEmpty() && nowMillis - samples.peekFirst().atMillis() >= windowMillis) {
            samples.pollFirst();
        }
    }
}
