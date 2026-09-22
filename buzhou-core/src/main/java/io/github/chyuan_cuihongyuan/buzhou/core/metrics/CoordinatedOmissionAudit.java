package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 协同遗漏校正审计（spec 1879 / T2959 / impl 1480）——Gil Tene
 * Coordinated Omission：固定速率发送下一个慢响应会「协同」推迟后续
 * 发送，停顿期本会观察到的延迟样本从未存在——原始分位数系统性偏
 * 乐观。expectedInterval 阶梯补账（HdrHistogram 语义）：慢响应按
 * 期望间隔下行的阶梯补记，被掩盖的发送机会重新进入账面。
 *
 * <p>纯函数零状态；阶梯只计数不物化（补记值 = latency − k×interval）。
 */
public final class CoordinatedOmissionAudit {

    private CoordinatedOmissionAudit() {
    }

    /**
     * 校正样本数：该慢响应应补记的样本总数（观测 1 + 阶梯补记）。
     * latency ≤ interval 即 1（无遗漏）；450/100 → 450,350,250,150 = 4。
     * 契约：latency ≥ 0、interval ≥ 1（fail-fast）。
     */
    public static long correctedSampleCount(long observedLatencyMillis,
                                            long expectedIntervalMillis) {
        validate(observedLatencyMillis, expectedIntervalMillis);
        if (observedLatencyMillis <= expectedIntervalMillis) {
            return 1;
        }
        return 1 + (observedLatencyMillis - expectedIntervalMillis)
                / expectedIntervalMillis;
    }

    /**
     * 遗漏数：本次慢响应掩盖的发送机会 = 校正样本数 − 1。
     */
    public static long omittedCount(long observedLatencyMillis,
                                    long expectedIntervalMillis) {
        return correctedSampleCount(observedLatencyMillis, expectedIntervalMillis) - 1;
    }

    /**
     * 协同静默窗长：latency − interval 与 0 取大——慢响应让发送方
     * 「以为系统没事」的时长。
     */
    public static long blindWindow(long observedLatencyMillis,
                                   long expectedIntervalMillis) {
        validate(observedLatencyMillis, expectedIntervalMillis);
        return Math.max(0, observedLatencyMillis - expectedIntervalMillis);
    }

    /**
     * 覆盖率：原始记录覆盖真实需求的比例（observed/corrected，1.0 =
     * 无盲区）。契约：correctedSamples ≥ 1 且 ≥ observedSamples
     * （补账不可能减样本，fail-fast）。
     */
    public static double coverageRatio(long observedSamples, long correctedSamples) {
        if (correctedSamples < 1) {
            throw new IllegalArgumentException(
                    "correctedSamples 不能小于 1：" + correctedSamples);
        }
        if (observedSamples < 0 || observedSamples > correctedSamples) {
            throw new IllegalArgumentException(String.format(
                    "观测样本须在 [0, 校正=%d]：%d", correctedSamples, observedSamples));
        }
        return (double) observedSamples / correctedSamples;
    }

    private static void validate(long observedLatencyMillis,
                                 long expectedIntervalMillis) {
        if (observedLatencyMillis < 0) {
            throw new IllegalArgumentException(
                    "时延不能为负：" + observedLatencyMillis);
        }
        if (expectedIntervalMillis < 1) {
            throw new IllegalArgumentException(
                    "expectedIntervalMillis 不能小于 1：" + expectedIntervalMillis);
        }
    }
}
