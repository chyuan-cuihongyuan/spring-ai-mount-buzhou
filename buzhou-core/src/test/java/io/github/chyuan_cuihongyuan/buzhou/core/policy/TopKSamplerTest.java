package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3038 / T5078：top-k+温度合同——k=1 恒 argmax、低温趋贪心、
 * 高温趋均匀、读数手算（4:1 → 0.8/0.2）、截断集外恒零、−∞ 永不
 * 中、同种子回放、参数 fail-fast。
 */
class TopKSamplerTest {

    private static final RandomGeneratorFactory<RandomGenerator> RNG_FACTORY =
            RandomGeneratorFactory.of("L64X256MixRandom");

    @Test
    void kEqualsOneShouldAlwaysPickArgmax() {
        double[] logits = {0.1, 3.0, 2.0};
        RandomGenerator rng = RNG_FACTORY.create(42);
        for (int i = 0; i < 1_000; i++) {
            assertThat(TopKSampler.sample(logits, 1, 1.0, rng)).isEqualTo(1);
        }
    }

    @Test
    void lowTemperatureShouldApproachGreedy() {
        double[] logits = {2.0, 3.0, 1.0};
        RandomGenerator rng = RNG_FACTORY.create(7);
        int argmaxHits = 0;
        for (int i = 0; i < 5_000; i++) {
            if (TopKSampler.sample(logits, 3, 0.01, rng) == 1) {
                argmaxHits++;
            }
        }
        assertThat(argmaxHits).isGreaterThan(4_900);
    }

    @Test
    void highTemperatureShouldApproachUniform() {
        double[] logits = {2.0, 3.0, 1.0};
        RandomGenerator rng = RNG_FACTORY.create(11);
        int[] counts = new int[3];
        for (int i = 0; i < 30_000; i++) {
            counts[TopKSampler.sample(logits, 3, 10_000, rng)]++;
        }
        for (int c : counts) {
            assertThat(c / 30_000.0).isCloseTo(1.0 / 3, within(0.02));
        }
    }

    @Test
    void probabilitiesShouldMatchHandComputation() {
        // logits {ln4, 0} T=1 k=2：softmax {4/(4+1), 1/5} = {0.8, 0.2}
        double[] probs = TopKSampler.probabilities(new double[] {Math.log(4), 0}, 2, 1.0);
        assertThat(probs[0]).isCloseTo(0.8, within(1e-12));
        assertThat(probs[1]).isCloseTo(0.2, within(1e-12));
    }

    @Test
    void outsideTopKShouldStayZeroProbability() {
        double[] probs = TopKSampler.probabilities(new double[] {10, 9, 1, 0}, 2, 1.0);
        assertThat(probs[2]).isZero();
        assertThat(probs[3]).isZero();
        assertThat(probs[0]).isGreaterThan(probs[1]);
    }

    @Test
    void minusInfinityShouldNeverBeSampled() {
        double[] logits = {1.0, Double.NEGATIVE_INFINITY, 2.0};
        RandomGenerator rng = RNG_FACTORY.create(3);
        for (int i = 0; i < 2_000; i++) {
            assertThat(TopKSampler.sample(logits, 3, 1.0, rng)).isNotEqualTo(1);
        }
    }

    @Test
    void sameSeedShouldReplay() {
        double[] logits = {1, 2, 3, 4};
        RandomGenerator a = RNG_FACTORY.create(99);
        RandomGenerator b = RNG_FACTORY.create(99);
        for (int i = 0; i < 100; i++) {
            assertThat(TopKSampler.sample(logits, 3, 0.7, a))
                    .isEqualTo(TopKSampler.sample(logits, 3, 0.7, b));
        }
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        RandomGenerator rng = RNG_FACTORY.create(1);
        assertThatThrownBy(() -> TopKSampler.sample(new double[0], 1, 1, rng))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TopKSampler.sample(new double[3], 0, 1, rng))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TopKSampler.sample(new double[3], 4, 1, rng))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TopKSampler.probabilities(new double[3], 2, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
