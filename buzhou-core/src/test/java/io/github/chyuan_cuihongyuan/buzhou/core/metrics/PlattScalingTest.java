package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 11016 / Y11033：PlattScaling 合同验证——已知参数生成数据复原
 * +序一致面+确定性+fail-fast。
 */
class PlattScalingTest {

    @Test
    void shouldRecoverParameters_whenGeneratedFromKnownSigmoid() {
        Random random = new Random(11016L);
        int n = 2000;
        double[] scores = new double[n];
        int[] labels = new int[n];
        for (int i = 0; i < n; i++) {
            scores[i] = random.nextGaussian() * 2;
            labels[i] = random.nextDouble() < 1.0 / (1.0 + Math.exp(-(2.0 * scores[i] - 1.0)))
                    ? 1 : 0;
        }
        PlattScaling scaling = PlattScaling.fit(scores, labels, 100);
        assertThat(scaling.parameters().a()).isBetween(1.5, 2.6);
        assertThat(scaling.parameters().b()).isBetween(-1.5, -0.5);
    }

    @Test
    void shouldBeRankConsistent_whenPerfectlySeparable() {
        double[] scores = {-5, -3, -1, 1, 3, 5};
        int[] labels = {0, 0, 0, 1, 1, 1};
        PlattScaling scaling = PlattScaling.fit(scores, labels, 100);
        assertThat(scaling.parameters().a()).isGreaterThan(0.0);
        for (int i = 1; i < scores.length; i++) {
            assertThat(scaling.probability(scores[i]))
                    .isGreaterThanOrEqualTo(scaling.probability(scores[i - 1]));
        }
    }

    @Test
    void shouldCenterBoundary_whenSymmetricData() {
        Random random = new Random(7L);
        int n = 2000;
        double[] scores = new double[n];
        int[] labels = new int[n];
        for (int i = 0; i < n; i++) {
            scores[i] = random.nextGaussian() * 2;
            labels[i] = random.nextDouble() < 1.0 / (1.0 + Math.exp(-scores[i])) ? 1 : 0;
        }
        PlattScaling scaling = PlattScaling.fit(scores, labels, 100);
        assertThat(scaling.parameters().b()).isBetween(-0.3, 0.3);
    }

    @Test
    void shouldReproduceIdenticalParameters_whenSameInputTwice() {
        double[] scores = {-2, -1, 0, 1, 2};
        int[] labels = {0, 0, 1, 1, 1};
        PlattScaling first = PlattScaling.fit(scores, labels, 50);
        PlattScaling second = PlattScaling.fit(scores, labels, 50);
        assertThat(second.parameters()).isEqualTo(first.parameters());
    }

    @Test
    void shouldFailFast_whenInvalidInputs() {
        double[] scores = {1, 2};
        assertThatThrownBy(() -> PlattScaling.fit(scores, new int[]{0}, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("长度不配");
        assertThatThrownBy(() -> PlattScaling.fit(scores, new int[]{0, 2}, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("{0,1}");
        assertThatThrownBy(() -> PlattScaling.fit(scores, new int[]{0, 1}, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("迭代数");
    }
}
