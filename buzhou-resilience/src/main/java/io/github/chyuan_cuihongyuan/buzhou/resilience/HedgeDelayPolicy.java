package io.github.chyuan_cuihongyuan.buzhou.resilience;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 对冲延迟策略（spec 1831 / T2863 / impl 1432）——Google《The Tail at
 * Scale》hedged requests 思想：对冲请求不是立即并发发两份（双倍负载），
 * 而是**等到 P95 延迟仍未返回才发**——只在真正的尾部等待里付双倍钱，
 * 中位数请求零对冲成本。样本不足时退守地板值（无数据不冒进）。
 *
 * <p>纯函数零状态、只裁决不执行（对冲动作归宿主）；分位数用最近秩法
 *（nearest-rank，确定性无插值）。
 */
public final class HedgeDelayPolicy {

    /** 默认对冲分位（P95——尾部 5% 才值得付对冲钱）。 */
    public static final double DEFAULT_PERCENTILE = 0.95d;

    /** 默认地板延迟（毫秒）——样本不足时的保守退守值。 */
    public static final long DEFAULT_FLOOR_MILLIS = 10L;

    /** 分位数可信的最小样本数。 */
    public static final int DEFAULT_MIN_SAMPLES = 20;

    private HedgeDelayPolicy() {
    }

    /** 对冲裁决两态：WAIT 继续等 / SEND_HEDGE 发对冲。 */
    public enum HedgeDecision {

        /** 未达对冲阈——继续等原请求。 */
        WAIT,

        /** 已达阈仍未返回——发对冲请求。 */
        SEND_HEDGE
    }

    /**
     * 对冲阈值：samples ≥ minSamples 时取分位数（最近秩法），否则取地板
     * 值（保守退守）。契约：samples 非空且元素 ≥ 0、percentile ∈ (0,1]、
     * floorMillis ≥ 0（fail-fast）。
     */
    public static long hedgeThresholdMillis(List<Long> samples, double percentile,
                                            long floorMillis, int minSamples) {
        if (samples == null || samples.isEmpty()) {
            throw new IllegalArgumentException("samples 不能为空");
        }
        if (percentile <= 0 || percentile > 1 || Double.isNaN(percentile)) {
            throw new IllegalArgumentException("percentile 须在 (0,1]：" + percentile);
        }
        if (floorMillis < 0) {
            throw new IllegalArgumentException("floorMillis 不能为负：" + floorMillis);
        }
        List<Long> sorted = new ArrayList<>(samples);
        for (Long v : sorted) {
            if (v == null || v < 0) {
                throw new IllegalArgumentException("延迟样本不能为 null 或负");
            }
        }
        sorted.sort(Comparator.naturalOrder());
        if (sorted.size() < minSamples) {
            return floorMillis;
        }
        long rank = (long) Math.ceil(percentile * sorted.size());
        return sorted.get((int) Math.min(Math.max(rank, 1), sorted.size()) - 1);
    }

    /**
     * 对冲裁决：elapsed ≥ threshold 即 SEND_HEDGE（边界含——到点就发，
     * 不再观望）。契约：两者 ≥ 0。
     */
    public static HedgeDecision decide(long elapsedMillis, long thresholdMillis) {
        if (elapsedMillis < 0 || thresholdMillis < 0) {
            throw new IllegalArgumentException(String.format(
                    "入参不能为负：elapsed=%d, threshold=%d", elapsedMillis, thresholdMillis));
        }
        return elapsedMillis >= thresholdMillis
                ? HedgeDecision.SEND_HEDGE
                : HedgeDecision.WAIT;
    }
}
