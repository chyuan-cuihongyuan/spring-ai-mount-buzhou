package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import java.util.Arrays;

/**
 * 自适应抖动缓冲（spec 1903 / T3007 / impl 1504）——VoIP/WebRTC
 * adaptive jitter buffer 语义：事件到达间隔抖动时，播放端延迟 D 再
 * 出队，D 按「近期到达延迟的覆盖分位」自适应——D 太小事件未到即
 * 播（空转），D 太大延迟白白增加；target 覆盖率是两者的换挡杆。
 *
 * <p>纯函数零状态（样本窗口归调用方）。
 */
public final class JitterBuffer {

    private JitterBuffer() {
    }

    /**
     * 覆盖 target 比例所需的最小延迟：升序样本第 ⌈target×n⌉ 个
     * （下标 ⌈target×n⌉−1——覆盖保证下取达标样本）。契约：样本非空
     * 且逐值 ≥ 0、target ∈ (0,1]（fail-fast）。
     */
    public static long requiredDelay(long[] arrivalDelaysMillis,
                                     double targetCoverage) {
        long[] sorted = validate(arrivalDelaysMillis, targetCoverage);
        int index = (int) Math.ceil(targetCoverage * sorted.length) - 1;
        return sorted[Math.max(0, index)];
    }

    /**
     * 实际覆盖读数：样本 ≤ delayMillis 的占比（0.0–1.0）。
     */
    public static double coverageRatio(long[] arrivalDelaysMillis,
                                       long delayMillis) {
        if (arrivalDelaysMillis == null || arrivalDelaysMillis.length == 0) {
            throw new IllegalArgumentException("样本表不能为空");
        }
        if (delayMillis < 0) {
            throw new IllegalArgumentException("delay 不能为负：" + delayMillis);
        }
        long covered = Arrays.stream(arrivalDelaysMillis)
                .filter(d -> d <= delayMillis)
                .count();
        return (double) covered / arrivalDelaysMillis.length;
    }

    private static long[] validate(long[] arrivalDelaysMillis,
                                   double targetCoverage) {
        if (arrivalDelaysMillis == null || arrivalDelaysMillis.length == 0) {
            throw new IllegalArgumentException("样本表不能为空");
        }
        if (targetCoverage <= 0.0 || targetCoverage > 1.0) {
            throw new IllegalArgumentException(
                    "targetCoverage 须在 (0,1]：" + targetCoverage);
        }
        for (long d : arrivalDelaysMillis) {
            if (d < 0) {
                throw new IllegalArgumentException("到达延迟不能为负：" + d);
            }
        }
        long[] sorted = arrivalDelaysMillis.clone();
        Arrays.sort(sorted);
        return sorted;
    }
}
