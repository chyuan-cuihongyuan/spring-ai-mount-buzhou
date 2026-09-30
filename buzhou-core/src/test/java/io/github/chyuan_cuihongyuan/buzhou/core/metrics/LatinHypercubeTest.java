package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * LatinHypercube 契约测试（spec 10026 / X10054）：每层恰一点圣像
 * + 值域 + 种子确定性 + 异种子异样 + fail-fast。
 */
class LatinHypercubeTest {

    @Test
    void shouldPlaceExactlyOnePointPerStratumPerDimension() {
        int samples = 20;
        int dimensions = 3;
        double[][] points = LatinHypercube.sample(dimensions, samples, 42L);
        for (int d = 0; d < dimensions; d++) {
            boolean[] occupied = new boolean[samples];
            for (double[] point : points) {
                int stratum = (int) (point[d] * samples);
                assertThat(occupied[stratum]).as("维 %d 层 %d 恰一点", d, stratum).isFalse();
                occupied[stratum] = true;
            }
            for (boolean used : occupied) {
                assertThat(used).as("维 %d 全层覆盖", d).isTrue();
            }
        }
    }

    @Test
    void shouldBeInUnitHypercube() {
        double[][] points = LatinHypercube.sample(4, 30, 7L);
        for (double[] point : points) {
            for (double v : point) {
                assertThat(v).isBetween(0.0, 1.0);
            }
        }
    }

    @Test
    void shouldBeDeterministicPerSeedAndDifferAcrossSeeds() {
        double[][] first = LatinHypercube.sample(2, 16, 99L);
        double[][] second = LatinHypercube.sample(2, 16, 99L);
        double[][] other = LatinHypercube.sample(2, 16, 100L);
        assertThat(java.util.Arrays.deepEquals(first, second)).isTrue();
        boolean differs = false;
        for (int i = 0; i < first.length && !differs; i++) {
            for (int d = 0; d < 2; d++) {
                if (Double.doubleToLongBits(first[i][d]) != Double.doubleToLongBits(other[i][d])) {
                    differs = true;
                    break;
                }
            }
        }
        assertThat(differs).as("异种子异样").isTrue();
    }

    @Test
    void shouldFailFastOnContractViolations() {
        assertThatThrownBy(() -> LatinHypercube.sample(0, 10, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LatinHypercube.sample(2, 0, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LatinHypercube.sample(-1, 10, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
