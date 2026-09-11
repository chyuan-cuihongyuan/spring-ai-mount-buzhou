package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 成本异常尖峰检测（spec 508 / T767，Prometheus/Istio 滚动基线 z-score
 * 思想——与 403 forecast 互补：forecast 回答趋势、spike 回答突刺）：
 * 当前分钟桶 vs 基线（前 baselineBuckets 个已完成桶均值/标准差）→
 * z ≥ 阈值且 ≥ 绝对地板且 minSamples 满足 → SpikeEvent listener +
 * 计数；cooldown 防抖（默认 5min 一发）。评估仅在 record 时触发
 * （零花费零成本）。
 *
 * <p>诚实边界：零方差基线（全 0/全等值）下 current>mean 视为 z=∞
 * （地板与 minSamples 兜噪）；重启历史清零（403 同注记）；只检测不拦截
 * （335 冻结可作下游联动）。
 */
public final class CostSpikeDetector {

    /** 尖峰事件（当前桶与基线统计——观测/告警载荷）。 */
    public record SpikeEvent(long currentBucketMicroUsd, double baselineMeanMicroUsd,
            double baselineStdDev, double zScore) {
    }

    private final SpendRateRing ring;
    private final int baselineBuckets;
    private final int minSamples;
    private final double zThreshold;
    private final long floorMicroUsd;
    private final long cooldownMinutes;
    private final Clock clock;
    private volatile Consumer<SpikeEvent> listener;
    private final AtomicLong spikes = new AtomicLong();
    private long lastSpikeEpochMinute = Long.MIN_VALUE;

    public CostSpikeDetector(SpendRateRing ring, int baselineBuckets, int minSamples,
            double zThreshold, long floorMicroUsd, Duration cooldown, Clock clock) {
        if (baselineBuckets < 2 || minSamples < 1 || minSamples > baselineBuckets) {
            throw new IllegalArgumentException(
                    "基线桶数 >=2 且 1 <= min-samples <= baseline-buckets");
        }
        if (zThreshold <= 0 || floorMicroUsd < 0) {
            throw new IllegalArgumentException("z-threshold > 0、floor-micro-usd >= 0");
        }
        this.ring = ring;
        this.baselineBuckets = baselineBuckets;
        this.minSamples = minSamples;
        this.zThreshold = zThreshold;
        this.floorMicroUsd = floorMicroUsd;
        Duration effectiveCooldown = cooldown == null ? Duration.ofMinutes(5) : cooldown;
        this.cooldownMinutes = Math.max(1, effectiveCooldown.toMinutes());
        this.clock = clock;
    }

    /** 尖峰监听（告警/联动下游；单监听——多路自行分叉）。 */
    public void onSpike(Consumer<SpikeEvent> listener) {
        this.listener = listener;
    }

    /** 记一笔花费并评估（与 403 喂缝同源——ModelCostLedger 监听喂数）。 */
    public void record(long microUsd) {
        ring.record(microUsd);
        evaluate();
    }

    /** 当前基线统计评估：尖峰判据全过 → 触发（cooldown 内忽略）。 */
    public synchronized void evaluate() {
        long current = ring.currentBucketTotal();
        if (current < floorMicroUsd) {
            return; // 噪声地板——零附近波动不检测
        }
        long[] baseline = ring.completedBuckets(baselineBuckets);
        if (baseline.length < minSamples) {
            return; // 基线样本不足
        }
        double mean = 0;
        for (long v : baseline) {
            mean += v;
        }
        mean /= baseline.length;
        double variance = 0;
        for (long v : baseline) {
            variance += (v - mean) * (v - mean);
        }
        double std = Math.sqrt(variance / baseline.length);
        double z;
        if (std == 0) {
            z = current > mean ? Double.POSITIVE_INFINITY : 0;
        } else {
            z = (current - mean) / std;
        }
        if (z < zThreshold) {
            return;
        }
        long minute = clock.millis() / 60_000L;
        if (lastSpikeEpochMinute != Long.MIN_VALUE
                && minute - lastSpikeEpochMinute < cooldownMinutes) {
            return; // 冷却窗口——防抖
        }
        lastSpikeEpochMinute = minute;
        spikes.incrementAndGet();
        BuzhouMetricsHolder.metrics().counter("buzhou.budget.cost-spike");
        Consumer<SpikeEvent> sink = listener;
        if (sink != null) {
            sink.accept(new SpikeEvent(current, mean, std, z));
        }
    }

    public long spikeCount() {
        return spikes.get();
    }
}
