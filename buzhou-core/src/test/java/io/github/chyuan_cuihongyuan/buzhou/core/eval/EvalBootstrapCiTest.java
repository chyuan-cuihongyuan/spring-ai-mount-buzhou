package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * impl-656 / spec 903：bootstrap 均值置信区间——同 seed 可复现、区间有序且
 * 含点估计（对称分布）、覆盖性近似（单峰样本 95% 区间应远窄于极差）、单样本
 * 退化、参数校验。
 */
class EvalBootstrapCiTest {

    @Test
    void sameSeedReproducesExactInterval() {
        double[] samples = {0.1, 0.5, 0.8, 0.3, 0.6, 0.4, 0.9, 0.2};
        EvalScoreAnalytics.MeanInterval a =
                EvalScoreAnalytics.bootstrapMeanInterval(samples, 0.95, 1000, 42L);
        EvalScoreAnalytics.MeanInterval b =
                EvalScoreAnalytics.bootstrapMeanInterval(samples, 0.95, 1000, 42L);
        assertThat(a.lower()).isEqualTo(b.lower());
        assertThat(a.upper()).isEqualTo(b.upper());
    }

    @Test
    void intervalOrderedAndContainsPointEstimateForSymmetricSample() {
        double[] samples = {0.2, 0.4, 0.6, 0.8}; // 对称分布：点估计应落在区间内
        EvalScoreAnalytics.MeanInterval ci =
                EvalScoreAnalytics.bootstrapMeanInterval(samples, 0.95, 2000, 7L);
        assertThat(ci.lower()).isLessThanOrEqualTo(ci.upper());
        assertThat(ci.pointEstimate()).isCloseTo(0.5, within(1e-12));
        assertThat(ci.lower()).isLessThanOrEqualTo(ci.pointEstimate());
        assertThat(ci.pointEstimate()).isLessThanOrEqualTo(ci.upper());
    }

    @Test
    void intervalTighterThanRangeForPeakedSample() {
        double[] samples = new double[60];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = 0.5 + (i % 5) * 0.001; // 集中在 [0.5, 0.504]
        }
        EvalScoreAnalytics.MeanInterval ci =
                EvalScoreAnalytics.bootstrapMeanInterval(samples, 0.95, 2000, 1L);
        assertThat(ci.upper() - ci.lower()).isLessThan(0.01); // 远窄于理论极差
        assertThat(ci.lower()).isGreaterThanOrEqualTo(0.5 - 0.01);
        assertThat(ci.upper()).isLessThanOrEqualTo(0.504 + 0.01);
    }

    @Test
    void singleSampleDegeneratesToPointInterval() {
        EvalScoreAnalytics.MeanInterval ci =
                EvalScoreAnalytics.bootstrapMeanInterval(new double[]{0.42}, 0.95, 100, 3L);
        assertThat(ci.lower()).isEqualTo(ci.upper()).isEqualTo(0.42);
    }

    @Test
    void argsValidated() {
        assertThatThrownBy(() ->
                EvalScoreAnalytics.bootstrapMeanInterval(new double[0], 0.95, 100, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                EvalScoreAnalytics.bootstrapMeanInterval(new double[]{Double.NaN}, 0.95, 100, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                EvalScoreAnalytics.bootstrapMeanInterval(new double[]{0.5}, 1.0, 100, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                EvalScoreAnalytics.bootstrapMeanInterval(new double[]{0.5}, 0.95, 0, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
