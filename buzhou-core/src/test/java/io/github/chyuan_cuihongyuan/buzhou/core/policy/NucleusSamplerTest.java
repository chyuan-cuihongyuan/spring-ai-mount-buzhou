package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3004 / T5010：top-p 核采样合同——核大小手算阶梯、p→0 退化
 * top-1、p=1 全覆盖、核内重归一经验频率（种子回放）、同种子确定性、
 * −∞ 永不中、参数诚实拒绝。
 */
class NucleusSamplerTest {

    /** logits = ln({0.5, 0.3, 0.15, 0.05})——softmax 还原该分布。 */
    private static double[] knownLogits() {
        return new double[] {Math.log(0.5), Math.log(0.3), Math.log(0.15), Math.log(0.05)};
    }

    private static final RandomGeneratorFactory<RandomGenerator> RNG_FACTORY =
            RandomGeneratorFactory.of("L64X256MixRandom");

    @Test
    void keptCountShouldMatchHandComputationLadder() {
        double[] logits = knownLogits();
        // 累积阶梯 0.5 / 0.8 / 0.95 / 1.0
        assertThat(NucleusSampler.keptCount(logits, 0.1)).isEqualTo(1);
        assertThat(NucleusSampler.keptCount(logits, 0.5)).isEqualTo(1);
        assertThat(NucleusSampler.keptCount(logits, 0.6)).isEqualTo(2);
        assertThat(NucleusSampler.keptCount(logits, 0.9)).isEqualTo(3);
        assertThat(NucleusSampler.keptCount(logits, 0.95)).isEqualTo(3);
        assertThat(NucleusSampler.keptCount(logits, 1.0)).isEqualTo(4);
    }

    @Test
    void tinyPShouldCollapseToArgmax() {
        double[] logits = knownLogits();
        RandomGenerator rng = RNG_FACTORY.create(42);
        for (int i = 0; i < 1_000; i++) {
            assertThat(NucleusSampler.sample(logits, 1e-6, rng)).isZero();
        }
    }

    @Test
    void empiricalFrequenciesShouldTrackRenormalizedNucleus() {
        // p=0.6 → 核 {0.5, 0.3} 重归一 {0.625, 0.375}——idx2/3 恒不中
        double[] logits = knownLogits();
        RandomGenerator rng = RNG_FACTORY.create(42);
        int[] counts = new int[4];
        int draws = 10_000;
        for (int i = 0; i < draws; i++) {
            counts[NucleusSampler.sample(logits, 0.6, rng)]++;
        }
        assertThat(counts[0] / (double) draws).isCloseTo(0.625, within(0.02));
        assertThat(counts[1] / (double) draws).isCloseTo(0.375, within(0.02));
        assertThat(counts[2]).isZero();
        assertThat(counts[3]).isZero();
    }

    @Test
    void fullPShouldCoverAllFiniteItems() {
        double[] logits = {0.0, Math.log(2), Double.NEGATIVE_INFINITY};
        RandomGenerator rng = RNG_FACTORY.create(7);
        boolean[] seen = new boolean[3];
        for (int i = 0; i < 2_000; i++) {
            int idx = NucleusSampler.sample(logits, 1.0, rng);
            seen[idx] = true;
            assertThat(idx).isNotEqualTo(2);
        }
        assertThat(seen[0]).isTrue();
        assertThat(seen[1]).isTrue();
    }

    @Test
    void sameSeedShouldReplaySameSequence() {
        double[] logits = knownLogits();
        RandomGenerator a = RNG_FACTORY.create(99);
        RandomGenerator b = RNG_FACTORY.create(99);
        for (int i = 0; i < 100; i++) {
            assertThat(NucleusSampler.sample(logits, 0.9, a))
                    .isEqualTo(NucleusSampler.sample(logits, 0.9, b));
        }
    }

    @Test
    void minusInfinityShouldNeverBeSampled() {
        // −∞ 质量为零：与有限 logit 并列时同样永不中（禁选免掩码）
        double[] logits = {Math.log(0.5), Double.NEGATIVE_INFINITY, Math.log(0.5)};
        RandomGenerator rng = RNG_FACTORY.create(1);
        for (int i = 0; i < 500; i++) {
            assertThat(NucleusSampler.sample(logits, 1.0, rng)).isNotEqualTo(1);
        }
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        RandomGenerator rng = RNG_FACTORY.create(3);
        assertThatThrownBy(() -> NucleusSampler.sample(new double[0], 0.9, rng))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NucleusSampler.sample(knownLogits(), 0, rng))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NucleusSampler.sample(knownLogits(), 1.5, rng))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NucleusSampler.keptCount(knownLogits(), -0.1))
                .isInstanceOf(IllegalArgumentException.class);
        double[] allInfinite = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        assertThatThrownBy(() -> NucleusSampler.keptCount(allInfinite, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
