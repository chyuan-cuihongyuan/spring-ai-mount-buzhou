package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.Arrays;

/**
 * 时钟抖动测量（spec 1921 / T3043 / impl 1522）——NTP 时钟
 * discipline 惯例：偏斜（skew，恒定差）与抖动（jitter，差的方差）
 * 是两个量。偏斜可校（ClockSkewClamp 钳位），抖动只可测——抖动
 * 多大时钳位校正不可信、该告警换源，前置仪表。
 *
 * <p>持态小 keeper：定长滚动窗口；样本 < 2 哨兵 -1.0（诚实「样本
 * 不足」）。
 */
public final class ClockJitterMeter {

    private final long[] window;
    private int count;

    /**
     * @param windowSize 滚动窗口采样数（≥ 2，fail-fast）
     */
    public ClockJitterMeter(int windowSize) {
        if (windowSize < 2) {
            throw new IllegalArgumentException(
                    "windowSize 不能小于 2：" + windowSize);
        }
        this.window = new long[windowSize];
        this.count = 0;
    }

    /**
     * 记录一次本地-参考时钟偏差采样（可负——时钟可快可慢）。
     */
    public void record(long offsetMillis) {
        if (count < window.length) {
            window[count++] = offsetMillis;
            return;
        }
        // 满窗滚动：覆盖最旧
        System.arraycopy(window, 1, window, 0, window.length - 1);
        window[window.length - 1] = offsetMillis;
    }

    /**
     * 抖动读数：窗口内偏差总体标准差。样本 < 2 → -1.0（样本不足
     * 哨兵——诚实不足而非谎报 0）。
     */
    public double jitterMillis() {
        if (count < 2) {
            return -1.0;
        }
        double mean = meanOffsetMillis();
        double sq = 0;
        for (int i = 0; i < count; i++) {
            sq += (window[i] - mean) * (window[i] - mean);
        }
        return Math.sqrt(sq / count);
    }

    /**
     * 平均偏差读数（偏斜分量，符号即快慢方向）。
     */
    public double meanOffsetMillis() {
        if (count == 0) {
            return -1.0;
        }
        long sum = 0;
        for (int i = 0; i < count; i++) {
            sum += window[i];
        }
        return (double) sum / count;
    }

    /** 当前窗口样本数（诊断读数）。 */
    public int samples() {
        return count;
    }

    /** 窗口容量（诊断读数）。 */
    public int capacity() {
        return window.length;
    }

    /** 排序副本（诊断用，防御性拷贝）。 */
    public long[] snapshot() {
        return Arrays.copyOf(window, count);
    }
}
