package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 11015 / Y11031：IsotonicCalibration 合同验证——手锚+单调恒等
 * +递减全均值+总和守恒圣像+非降性+确定性+fail-fast。
 */
class IsotonicCalibrationTest {

    @Test
    void shouldPoolViolation_whenClassicHandAnchor() {
        assertThat(IsotonicCalibration.fit(new double[]{3, 1, 2}))
                .containsExactly(2.0, 2.0, 2.0);
    }

    @Test
    void shouldBeIdentity_whenAlreadyMonotone() {
        double[] values = {1, 2, 2, 5, 9};
        assertThat(IsotonicCalibration.fit(values)).containsExactly(values);
    }

    @Test
    void shouldFlatMean_whenStrictlyDecreasing() {
        double[] values = {4, 3, 2, 1};
        assertThat(IsotonicCalibration.fit(values))
                .containsExactly(2.5, 2.5, 2.5, 2.5);
    }

    @Test
    void shouldConserveSumAndStayMonotone_whenRandomInput() {
        Random random = new Random(11015L);
        double[] values = new double[50];
        double inputSum = 0;
        for (int i = 0; i < 50; i++) {
            values[i] = random.nextGaussian();
            inputSum += values[i];
        }
        double[] fitted = IsotonicCalibration.fit(values);
        double outputSum = 0;
        for (int i = 0; i < 50; i++) {
            outputSum += fitted[i];
            if (i > 0) {
                assertThat(fitted[i]).as("位 %d 非降", i)
                        .isGreaterThanOrEqualTo(fitted[i - 1] - 1e-12);
            }
        }
        assertThat(outputSum).isCloseTo(inputSum, within(1e-9));
    }

    @Test
    void shouldReproduceIdenticalOutput_whenSameInputTwice() {
        double[] values = {5, 1, 4, 2, 3};
        assertThat(IsotonicCalibration.fit(values)).isEqualTo(IsotonicCalibration.fit(values));
    }

    @Test
    void shouldFailFast_whenInvalidInput() {
        assertThatThrownBy(() -> IsotonicCalibration.fit(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IsotonicCalibration.fit(new double[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非空");
        double[] withNaN = {1, Double.NaN};
        assertThatThrownBy(() -> IsotonicCalibration.fit(withNaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非有限");
    }
}
