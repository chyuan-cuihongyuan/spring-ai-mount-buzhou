package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.util.List;

/**
 * 驱逐信号阈值门（spec 1808 / T2817 / impl 1409）——K8s eviction manager
 * 思想：驱逐信号分**硬阈值**（越过立即逐——保护节点存活的最后闸）与
 * **软阈值**（越过进入宽限期，宽限满仍在线才逐——给自发回收留时间窗），
 * 两级阈值避免「一刀切阈值」在临界点抖动驱逐。映射到 spill：信号可为
 * 配额占用率/句柄数/磁盘水位，越过硬阈立即逐最冷 handle、软阈给宿主一个
 * 可预期的宽限窗先行自救。
 *
 * <p>纯函数零状态、只裁决不执行（驱逐动作归宿主）；信号口径由调用方声明。
 */
public final class EvictionThresholdGate {

    private EvictionThresholdGate() {
    }

    /** 三态裁决：低于两阈 / 软阈宽限中（未到期）/ 立即逐（硬阈或宽限满）。 */
    public enum Decision {

        /** 低于软阈——无事发生。 */
        BELOW,

        /** 软阈之上但宽限未满——待观察（宿主可自发回收自救）。 */
        GRACE_PENDING,

        /** 硬阈之上或宽限满——立即逐。 */
        EVICT_NOW
    }

    /** 阈值对契约：0 ≤ soft ≤ hard（软阈不高于硬阈；相等即单阈退化）。 */
    public record Thresholds(double soft, double hard) {

        public Thresholds {
            if (Double.isNaN(soft) || Double.isNaN(hard) || soft < 0 || hard < soft) {
                throw new IllegalArgumentException(
                        "非法阈值对：soft=" + soft + ", hard=" + hard
                                + "（要求 0 ≤ soft ≤ hard 且非 NaN）");
            }
        }
    }

    /**
     * 单信号裁决。契约：signal 非 NaN、millisAboveSoft ≥ 0、graceMillis ≥ 0
     * （fail-fast）；语义：signal ≥ hard 立即逐；signal ≥ soft 看宽限（
     * millisAboveSoft ≥ graceMillis 即逐，否则待观察）；低于软阈 BELOW。
     */
    public static Decision decide(double signal, Thresholds thresholds,
                                  long millisAboveSoft, long graceMillis) {
        if (Double.isNaN(signal)) {
            throw new IllegalArgumentException("signal 不能为 NaN");
        }
        if (millisAboveSoft < 0 || graceMillis < 0) {
            throw new IllegalArgumentException(
                    "毫秒入参不能为负：millisAboveSoft=" + millisAboveSoft
                            + ", graceMillis=" + graceMillis);
        }
        if (signal >= thresholds.hard()) {
            return Decision.EVICT_NOW;
        }
        if (signal >= thresholds.soft()) {
            return millisAboveSoft >= graceMillis
                    ? Decision.EVICT_NOW
                    : Decision.GRACE_PENDING;
        }
        return Decision.BELOW;
    }

    /**
     * 裁决普查（多信号批量）。null 按空表；逐信号核契约，畸形即 fail-fast。
     */
    public static DecisionCensus census(Thresholds thresholds, long graceMillis,
                                        List<SignalSample> samples) {
        List<SignalSample> window = samples == null ? List.of() : samples;
        long below = 0;
        long pending = 0;
        long evict = 0;
        for (SignalSample s : window) {
            switch (decide(s.signalValue(), thresholds, s.millisAboveSoft(), graceMillis)) {
                case BELOW -> below++;
                case GRACE_PENDING -> pending++;
                case EVICT_NOW -> evict++;
            }
        }
        return new DecisionCensus(window.size(), below, pending, evict);
    }

    /** 单信号样本：信号现值 + 已在软阈之上的毫秒数（口径归宿主）。 */
    public record SignalSample(double signalValue, long millisAboveSoft) {
    }

    /** @param below/pending/evict 三态计数（合计 = signals） */
    public record DecisionCensus(int signals, long below, long pending, long evict) {

        /** 立即逐占比（无信号 -1 哨兵）。 */
        public double evictRatio() {
            return signals == 0 ? -1d : (double) evict / signals;
        }
    }
}
