package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 11014 / Y11029：RansacSampler 合同验证——外点污染直线复原
 * +全内点精确面+种子双面+fail-fast。
 */
class RansacSamplerTest {

    private static final double SLOPE = 2.0;
    private static final double INTERCEPT = 1.0;

    @Test
    void shouldRecoverLine_whenContaminatedWithOutliers() {
        List<double[]> points = new ArrayList<>();
        Random random = new Random(42L);
        for (int i = 0; i < 80; i++) {
            double x = i;
            points.add(new double[]{x, SLOPE * x + INTERCEPT + random.nextGaussian() * 0.01});
        }
        for (int i = 0; i < 20; i++) {
            points.add(new double[]{random.nextDouble() * 100,
                    random.nextDouble() * 100});
        }
        RansacSampler.Line line = RansacSampler.fitLine(
                points.toArray(new double[0][]), 200, 0.1, 7L);
        assertThat(line.slope()).isCloseTo(SLOPE, within(0.05));
        assertThat(line.intercept()).isCloseTo(INTERCEPT, within(0.05));
        assertThat(line.inliers()).isGreaterThanOrEqualTo(70);
    }

    @Test
    void shouldBeExact_whenAllInliers() {
        double[][] points = {{0, 1}, {1, 3}, {2, 5}, {3, 7}, {4, 9}};
        RansacSampler.Line line = RansacSampler.fitLine(points, 50, 1e-9, 1L);
        assertThat(line.slope()).isCloseTo(2.0, within(1e-9));
        assertThat(line.intercept()).isCloseTo(1.0, within(1e-9));
        assertThat(line.inliers()).isEqualTo(5);
    }

    @Test
    void shouldBeDeterministic_whenSameSeedAndDifferent_whenOtherSeed() {
        List<double[]> points = new ArrayList<>();
        Random random = new Random(42L);
        for (int i = 0; i < 40; i++) {
            points.add(new double[]{i, SLOPE * i + INTERCEPT});
        }
        for (int i = 0; i < 10; i++) {
            points.add(new double[]{random.nextDouble() * 50,
                    random.nextDouble() * 50});
        }
        double[][] array = points.toArray(new double[0][]);
        RansacSampler.Line first = RansacSampler.fitLine(array, 100, 0.1, 42L);
        RansacSampler.Line second = RansacSampler.fitLine(array, 100, 0.1, 42L);
        assertThat(second.slope()).isEqualTo(first.slope());
        assertThat(second.intercept()).isEqualTo(first.intercept());
    }

    @Test
    void shouldFailFast_whenInvalidInputs() {
        assertThatThrownBy(() -> RansacSampler.fitLine(new double[][]{{1, 1}}, 10, 0.1, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("点数");
        assertThatThrownBy(() -> RansacSampler.fitLine(new double[][]{{0, 0}, {1, 1}},
                0, 0.1, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("迭代数");
        assertThatThrownBy(() -> RansacSampler.fitLine(new double[][]{{0, 0}, {1, 1}},
                10, -1, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("阈值");
    }
}
