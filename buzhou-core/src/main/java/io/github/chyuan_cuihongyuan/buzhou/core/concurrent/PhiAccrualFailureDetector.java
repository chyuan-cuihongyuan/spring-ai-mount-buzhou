package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * φ 累积故障嫌疑度检测器（spec 2004 / T3109 / impl 1555）——
 * Hayashibara φ-accrual / Finagle-Akka 故障检测思想：心跳间隔历史建
 * 正态模型，φ = −log₁₀(P(此久未心跳 | 进程活着))——连续嫌疑度而非
 * 二值 up/down，调用方按阈值分档（1 怀疑、4+ 判定失联）。
 *
 * <p>滑动窗样本 + std 下界钳（防规律心跳 std→0 爆炸）+ φ 上界 12
 * （p 下界 1e−12）；时间全由调用方传入（确定性可回放）；synchronized
 * 小临界区。
 */
public final class PhiAccrualFailureDetector {

    /** φ 上界（p 下界 1e−12——防无限溢出）。 */
    public static final double MAX_PHI = 12.0d;

    /** 默认样本窗（个）。 */
    public static final int DEFAULT_WINDOW_SIZE = 1000;

    /** 默认 std 下界（毫秒）——规律心跳退化保护。 */
    public static final long DEFAULT_MIN_STD_DEV_MILLIS = 100L;

    private final int windowSize;
    private final double minStdDevMillis;
    private final Deque<Long> intervals = new ArrayDeque<>();
    private long lastHeartbeat = Long.MIN_VALUE;

    /** 契约：windowSize ≥ 2、minStdDevMillis &gt; 0（fail-fast）。 */
    public PhiAccrualFailureDetector(int windowSize, long minStdDevMillis) {
        if (windowSize < 2) {
            throw new IllegalArgumentException("windowSize 须 ≥ 2：" + windowSize);
        }
        if (minStdDevMillis <= 0) {
            throw new IllegalArgumentException("minStdDevMillis 须 > 0：" + minStdDevMillis);
        }
        this.windowSize = windowSize;
        this.minStdDevMillis = minStdDevMillis;
    }

    public PhiAccrualFailureDetector() {
        this(DEFAULT_WINDOW_SIZE, DEFAULT_MIN_STD_DEV_MILLIS);
    }

    /** 记一次心跳（nowMillis 单调不减——回拨 fail-fast；首个心跳只锚定不出样本）。 */
    public synchronized void heartbeat(long nowMillis) {
        if (nowMillis < lastHeartbeat) {
            throw new IllegalArgumentException("时间回拨：" + nowMillis + " < " + lastHeartbeat);
        }
        if (lastHeartbeat != Long.MIN_VALUE && nowMillis > lastHeartbeat) {
            intervals.addLast(nowMillis - lastHeartbeat);
            while (intervals.size() > windowSize) {
                intervals.pollFirst();
            }
        }
        lastHeartbeat = nowMillis;
    }

    /**
     * 当前嫌疑度 φ = −log₁₀(右尾概率)：样本 &lt; 2 恒 0（无统计诚实）；
     * p 钳下界 1e−12（φ ≤ 12）。从未心跳也恒 0（锚定前无语义）。
     */
    public synchronized double phi(long nowMillis) {
        if (intervals.size() < 2) {
            return 0.0d;
        }
        double mean = meanInterval();
        double std = Math.max(stdDev(mean), minStdDevMillis);
        double x = nowMillis - lastHeartbeat;
        double pRightTail = 0.5d * (1.0d - erf((x - mean) / (std * Math.sqrt(2.0d))));
        return -Math.log10(Math.max(pRightTail, 1e-12d));
    }

    /** 样本数读数（窗占满前 < windowSize）。 */
    public synchronized int sampleCount() {
        return intervals.size();
    }

    /** 间隔均值读数（毫秒；样本 &lt; 2 时 0）。 */
    public synchronized double meanIntervalMillis() {
        return intervals.size() < 2 ? 0.0d : meanInterval();
    }

    private double meanInterval() {
        long sum = 0;
        for (long v : intervals) {
            sum += v;
        }
        return (double) sum / intervals.size();
    }

    private double stdDev(double mean) {
        double sq = 0;
        for (long v : intervals) {
            double d = v - mean;
            sq += d * d;
        }
        return Math.sqrt(sq / intervals.size());
    }

    /** Abramowitz-Stegun 7.1.26 erf 有理近似（|误差| ≤ 1.5e−7——检测器精度足用）。 */
    private static double erf(double x) {
        double t = 1.0d / (1.0d + 0.3275911d * Math.abs(x));
        double y = 1.0d - (((((1.061405429d * t - 1.453152027d) * t) + 1.421413741d) * t
                - 0.284496736d) * t + 0.254829592d) * t * Math.exp(-x * x);
        return x >= 0 ? y : -y;
    }
}
