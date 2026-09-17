package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3026 / T5054：Box-Muller 合同——经验均值/标准差收敛 N(0,1)、
 * 平移缩放 N(μ,σ²)、成对两分量皆标准正态、1σ/2σ 覆盖率贴理论、
 * σ=0 常量、同种子回放、参数校验。
 */
class GaussianSamplerTest {

    private static final RandomGeneratorFactory<RandomGenerator> RNG_FACTORY =
            RandomGeneratorFactory.of("L64X256MixRandom");

    @Test
    void empiricalMomentsShouldConvergeToStandardNormal() {
        RandomGenerator rng = RNG_FACTORY.create(42);
        double sum = 0;
        double sumSquares = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            double z = GaussianSampler.sample(rng);
            sum += z;
            sumSquares += z * z;
        }
        double mean = sum / n;
        double stdDev = Math.sqrt(sumSquares / n - mean * mean);
        assertThat(mean).isCloseTo(0, within(0.02));
        assertThat(stdDev).isCloseTo(1, within(0.02));
    }

    @Test
    void shiftAndScaleShouldCarryThrough() {
        RandomGenerator rng = RNG_FACTORY.create(7);
        double sum = 0;
        double sumSquares = 0;
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            double x = GaussianSampler.sample(10, 2, rng);
            sum += x;
            sumSquares += x * x;
        }
        double mean = sum / n;
        double stdDev = Math.sqrt(sumSquares / n - mean * mean);
        assertThat(mean).isCloseTo(10, within(0.05));
        assertThat(stdDev).isCloseTo(2, within(0.05));
    }

    @Test
    void bothPairComponentsShouldBeStandardNormal() {
        RandomGenerator rng = RNG_FACTORY.create(11);
        double sum0 = 0;
        double sum1 = 0;
        int pairs = 50_000;
        for (int i = 0; i < pairs; i++) {
            double[] pair = GaussianSampler.samplePair(rng);
            sum0 += pair[0];
            sum1 += pair[1];
        }
        assertThat(sum0 / pairs).isCloseTo(0, within(0.03));
        assertThat(sum1 / pairs).isCloseTo(0, within(0.03));
    }

    @Test
    void sigmaCoverageShouldMatchTheory() {
        RandomGenerator rng = RNG_FACTORY.create(99);
        int n = 100_000;
        int withinOne = 0;
        int withinTwo = 0;
        for (int i = 0; i < n; i++) {
            double z = GaussianSampler.sample(rng);
            if (Math.abs(z) <= 1) {
                withinOne++;
            }
            if (Math.abs(z) <= 2) {
                withinTwo++;
            }
        }
        assertThat(withinOne / (double) n).isCloseTo(0.6827, within(0.01));
        assertThat(withinTwo / (double) n).isCloseTo(0.9545, within(0.01));
    }

    @Test
    void zeroSigmaShouldBeConstant() {
        RandomGenerator rng = RNG_FACTORY.create(5);
        for (int i = 0; i < 100; i++) {
            assertThat(GaussianSampler.sample(3.5, 0, rng)).isEqualTo(3.5);
        }
    }

    @Test
    void sameSeedShouldReplay() {
        RandomGenerator a = RNG_FACTORY.create(77);
        RandomGenerator b = RNG_FACTORY.create(77);
        for (int i = 0; i < 100; i++) {
            assertThat(GaussianSampler.sample(a)).isEqualTo(GaussianSampler.sample(b));
        }
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        RandomGenerator rng = RNG_FACTORY.create(1);
        assertThatThrownBy(() -> GaussianSampler.sample(0, -1, rng))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GaussianSampler.sample(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GaussianSampler.samplePair(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
