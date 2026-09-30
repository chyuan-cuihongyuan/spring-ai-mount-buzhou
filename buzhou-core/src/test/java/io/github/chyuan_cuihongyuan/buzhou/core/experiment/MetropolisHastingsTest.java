package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class MetropolisHastingsTest {

    private static double mean(double[] xs) {
        double sum = 0;
        for (double x : xs) {
            sum += x;
        }
        return sum / xs.length;
    }

    private static double sd(double[] xs) {
        double m = mean(xs);
        double sum = 0;
        for (double x : xs) {
            sum += (x - m) * (x - m);
        }
        return Math.sqrt(sum / (xs.length - 1));
    }

    @Test
    void shouldRecoverStandardNormalMoments() {
        // 圣像：logN(0,1) 后验——均值≈0、标准差≈1（MCMC 弱收敛界内）
        double[] samples = MetropolisHastings.sample(
                x -> -x * x / 2, 0, 1.0, 60000, 1000, new Random(11));
        assertThat(samples).hasSize(59000);
        assertThat(mean(samples)).isCloseTo(0.0, within(0.1));
        assertThat(sd(samples)).isCloseTo(1.0, within(0.15));
    }

    @Test
    void shouldCoverBothModesOfBimodalDensity() {
        // 双峰圣像：log(0.5N(−3,0.5²)+0.5N(3,0.5²))——两模均被覆盖且样本分裂
        double[] samples = MetropolisHastings.sample(
                x -> Math.log(Math.exp(-(x - 3) * (x - 3)) + Math.exp(-(x + 3) * (x + 3))),
                0, 2.0, 40000, 1000, new Random(23));
        long left = java.util.Arrays.stream(samples).filter(x -> x < 0).count();
        long right = samples.length - left;
        assertThat(left).isGreaterThan(5000);
        assertThat(right).isGreaterThan(5000);
        assertThat(mean(samples)).isCloseTo(0.0, within(0.3));
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        DoubleUnaryOperatorLike logDensity = x -> -x * x;
        assertThat(MetropolisHastings.sample(logDensity, 0, 1, 1000, 100, new Random(5)))
                .containsExactly(MetropolisHastings.sample(logDensity, 0, 1, 1000, 100, new Random(5)));
        assertThatThrownBy(() -> MetropolisHastings.sample(null, 0, 1, 100, 10, new Random()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MetropolisHastings.sample(logDensity, 0, 0, 100, 10, new Random()))
                .hasMessageContaining("步长");
        assertThatThrownBy(() -> MetropolisHastings.sample(logDensity, 0, 1, 0, 0, new Random()))
                .hasMessageContaining("抽样数");
        assertThatThrownBy(() -> MetropolisHastings.sample(logDensity, 0, 1, 100, 100, new Random()))
                .hasMessageContaining("烧入");
    }

    private interface DoubleUnaryOperatorLike extends java.util.function.DoubleUnaryOperator {
    }
}
