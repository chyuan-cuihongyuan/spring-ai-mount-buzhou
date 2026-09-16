package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;



import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2049 / T3200：Gumbel-max 采样合同——确定性回放、高频 logit 多中、
 * -∞ 永不中、均匀 logits 近均匀、频率对账 softmax、畸形 fail-fast。
 */
class GumbelMaxSamplerTest {

    @Test
    void sameSeedShouldReplaySameSequence() {
        java.util.random.RandomGenerator src1 = new java.util.SplittableRandom(42);
        java.util.random.RandomGenerator src2 = new java.util.SplittableRandom(42);
        GumbelMaxSampler sa = new GumbelMaxSampler(src1);
        GumbelMaxSampler sb = new GumbelMaxSampler(src2);
        double[] logits = {1.0d, 2.0d, 0.5d};
        for (int i = 0; i < 20; i++) {
            assertThat(sa.sampleIndex(logits)).isEqualTo(sb.sampleIndex(logits)); // 同种同序列
        }
    }

    @Test
    void dominantLogitShouldWinMostSamples() {
        GumbelMaxSampler sampler = new GumbelMaxSampler(new java.util.SplittableRandom(7));
        double[] logits = {10.0d, 0.0d, 0.0d}; // 强偏
        int dominant = 0;
        for (int i = 0; i < 1000; i++) {
            if (sampler.sampleIndex(logits) == 0) {
                dominant++;
            }
        }
        assertThat(dominant).isGreaterThan(900); // softmax(10,0,0) 主位 >99% 理论——工程宽界
    }

    @Test
    void negativeInfinityShouldNeverWin() {
        GumbelMaxSampler sampler = new GumbelMaxSampler(new java.util.SplittableRandom(9));
        double[] logits = {Double.NEGATIVE_INFINITY, 1.0d, Double.NEGATIVE_INFINITY};
        for (int i = 0; i < 500; i++) {
            assertThat(sampler.sampleIndex(logits)).isEqualTo(1); // 禁选永不中
        }
    }

    @Test
    void uniformLogitsShouldSpreadAcrossAll() {
        GumbelMaxSampler sampler = new GumbelMaxSampler(new java.util.SplittableRandom(11));
        double[] freq = sampler.empiricalFrequencies(new double[]{0, 0, 0, 0}, 10_000);
        for (double f : freq) {
            assertThat(f).isBetween(0.20d, 0.30d); // 均匀 ≈ 0.25 ± 宽界
        }
    }

    @Test
    void empiricalFrequencyShouldMatchSoftmax() {
        GumbelMaxSampler sampler = new GumbelMaxSampler(new java.util.SplittableRandom(13));
        // softmax(ln2, 0) = (2/3, 1/3)
        double[] freq = sampler.empiricalFrequencies(new double[]{Math.log(2), 0.0d}, 20_000);
        assertThat(freq[0]).isBetween(0.62d, 0.70d);
        assertThat(freq[1]).isBetween(0.30d, 0.38d);
    }

    @Test
    void malformedInputsShouldFailFast() {
        GumbelMaxSampler sampler = new GumbelMaxSampler(new java.util.SplittableRandom(1));
        assertThatThrownBy(() -> new GumbelMaxSampler(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sampler.sampleIndex(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sampler.sampleIndex(new double[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sampler.sampleIndex(new double[]{Double.NaN}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sampler.empiricalFrequencies(new double[]{1}, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
