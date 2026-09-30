package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class JackknifeEstimatorTest {

    private static double mean(double[] xs) {
        return Arrays.stream(xs).average().orElseThrow();
    }

    private static double variance(double[] xs) {
        double m = mean(xs);
        return Arrays.stream(xs).map(x -> (x - m) * (x - m)).sum() / (xs.length - 1);
    }

    @Test
    void shouldCarryMeanAnchorExactly() {
        // 均值的刀切：θ̄_loo == θ̂（均值无刀切偏差）→ bias=0、估计=原均值
        double[] sample = {1, 2, 3, 4, 5};
        JackknifeEstimator.Result result = JackknifeEstimator.estimate(sample, JackknifeEstimatorTest::mean);
        assertThat(result.bias()).isCloseTo(0.0, within(1e-12));
        assertThat(result.estimate()).isCloseTo(3.0, within(1e-12));
        // 均值刀切标准误 == s/√n（与解析公式恰等——经典不变量）
        assertThat(result.standardError()).isCloseTo(Math.sqrt(variance(sample) / sample.length), within(1e-12));
    }

    @Test
    void shouldReduceVarianceBias() {
        // 样本方差（n−1 分母）刀切偏差≈0；总体方差（n 分母）刀切后偏差被消
        double[] sample = {2.0, 4.0, 6.0, 8.0, 10.0};
        JackknifeEstimator.Result unbiased = JackknifeEstimator.estimate(sample, JackknifeEstimatorTest::variance);
        assertThat(unbiased.bias()).isCloseTo(0.0, within(1e-9));
        // n 分母方差低估真值——刀切估计应高于全样本估计（偏差消减方向圣像）
        JackknifeEstimator.Result biased = JackknifeEstimator.estimate(sample, xs -> {
            double m = mean(xs);
            return Arrays.stream(xs).map(x -> (x - m) * (x - m)).sum() / xs.length;
        });
        double full = 8.0; // {(−4+−2+0+2+4)²}/5 = 40/5 = 8
        assertThat(biased.estimate()).isGreaterThan(full);
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        double[] sample = {1.5, 2.5, 3.5, 4.5};
        JackknifeEstimator.Result first = JackknifeEstimator.estimate(sample, JackknifeEstimatorTest::mean);
        JackknifeEstimator.Result second = JackknifeEstimator.estimate(sample, JackknifeEstimatorTest::mean);
        assertThat(first.estimate()).isEqualTo(second.estimate());
        assertThat(first.standardError()).isEqualTo(second.standardError());
        assertThatThrownBy(() -> JackknifeEstimator.estimate(null, JackknifeEstimatorTest::mean))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JackknifeEstimator.estimate(sample, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JackknifeEstimator.estimate(new double[]{1}, JackknifeEstimatorTest::mean))
                .hasMessageContaining("样本 ≥2");
    }
}
