package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class PowerIterationTest {

    @Test
    void shouldFindDominantEigenvectorOfDiagonalMatrix() {
        double[][] matrix = {{4, 0}, {0, 1}};
        double[] result = PowerIteration.dominant(matrix, 200, 1e-12);
        assertThat(Math.abs(result[0])).isCloseTo(1.0, within(1e-9));
        assertThat(Math.abs(result[1])).isCloseTo(0.0, within(1e-6));
        assertThat(result[2]).isCloseTo(4.0, within(1e-9));
    }

    @Test
    void shouldMatchKnownEigenpairOfSymmetricMatrix() {
        double[][] matrix = {{2, 1}, {1, 2}};
        double[] result = PowerIteration.dominant(matrix, 500, 1e-14);
        assertThat(result[2]).isCloseTo(3.0, within(1e-9));
        assertThat(Math.abs(result[0] - result[1])).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        double[][] matrix = {{2, 1}, {1, 2}};
        double[] first = PowerIteration.dominant(matrix, 100, 1e-12);
        double[] second = PowerIteration.dominant(matrix, 100, 1e-12);
        assertThat(first).isEqualTo(second);
        assertThatThrownBy(() -> PowerIteration.dominant(null, 10, 1e-9))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PowerIteration.dominant(new double[][]{{1, 0}}, 10, 1e-9))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PowerIteration.dominant(new double[][]{{1, 0}, {0, 1}}, 0, 1e-9))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PowerIteration.dominant(new double[][]{{1, 0}, {0, 1}}, 10, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PowerIteration.dominant(new double[][]{{0, 0}, {0, 0}}, 10, 1e-9))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
