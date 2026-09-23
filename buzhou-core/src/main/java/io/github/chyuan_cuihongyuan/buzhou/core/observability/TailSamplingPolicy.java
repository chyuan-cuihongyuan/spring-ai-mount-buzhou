package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import java.util.Random;

/**
 * 尾采样策略（spec 4022 / T6045 / impl 2123）——完整轨迹再裁决
 * 思想（OpenTelemetry tail-based sampling；Jaeger adaptive 同族）：
 * 头采样在 span 起点盲抽（错误/慢轨迹与正常轨迹同概率被丢——
 * 排障金料恰最易丢），尾采样等**整条轨迹完成**再判——错误必采、
 * 慢轨迹必采（时延 ≥ 阈值含等）、其余按概率基线；概率分支受
 **采样预算**封顶（预算尽后只剩错误/慢通道——观测成本有硬顶而
 * 金料不丢）。
 *
 * <p>与头采样（起点一致性传播）互补：分布追踪的「金料全保 +
 * 成本可控」双档。随机源可注入（确定性回放）；计数面可审计。
 */
public final class TailSamplingPolicy {

    /** 裁决（采样与否 + 理由——error/slow/probabilistic/budget）。 */
    public record Verdict(boolean sampled, String reason) {
    }

    private final long slowThresholdMillis;
    private final double baseProbability;
    private final long maxProbabilistic;
    private final Random random;
    private long probabilisticSampled;
    private long errorSampled;
    private long slowSampled;
    private long dropped;

    /** 定构（threshold≥0、p∈[0,1]、maxProbabilistic≥0、random 非 null 否则 fail-fast）。 */
    public TailSamplingPolicy(long slowThresholdMillis, double baseProbability,
            long maxProbabilistic, Random random) {
        if (slowThresholdMillis < 0 || baseProbability < 0 || baseProbability > 1
                || maxProbabilistic < 0 || random == null) {
            throw new IllegalArgumentException("threshold≥0 / p∈[0,1] / max≥0 / random 非空："
                    + slowThresholdMillis + "/" + baseProbability + "/" + maxProbabilistic);
        }
        this.slowThresholdMillis = slowThresholdMillis;
        this.baseProbability = baseProbability;
        this.maxProbabilistic = maxProbabilistic;
        this.random = random;
    }

    /** 整条轨迹完成后的裁决（错误 > 慢 > 预算内概率 > 预算尽丢弃）。 */
    public Verdict evaluate(long durationMillis, boolean hasError) {
        if (durationMillis < 0) {
            throw new IllegalArgumentException("durationMillis≥0：" + durationMillis);
        }
        if (hasError) {
            errorSampled++;
            return new Verdict(true, "error");   // 金料通道不受预算影响
        }
        if (durationMillis >= slowThresholdMillis) {
            slowSampled++;
            return new Verdict(true, "slow");   // 慢轨迹必采（含等阈值）
        }
        if (probabilisticSampled >= maxProbabilistic) {
            dropped++;
            return new Verdict(false, "budget");   // 预算尽——正常轨迹让位
        }
        if (random.nextDouble() < baseProbability) {
            probabilisticSampled++;
            return new Verdict(true, "probabilistic");
        }
        dropped++;
        return new Verdict(false, "below-threshold");
    }

    /** 概率通道已采计数（预算面）。 */
    public long probabilisticSampled() {
        return probabilisticSampled;
    }

    /** 错误通道已采计数。 */
    public long errorSampled() {
        return errorSampled;
    }

    /** 慢通道已采计数。 */
    public long slowSampled() {
        return slowSampled;
    }

    /** 已丢弃计数（守恒账面）。 */
    public long dropped() {
        return dropped;
    }
}
