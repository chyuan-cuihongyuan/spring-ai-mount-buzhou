package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 11001 / Y11003：VanDerCorput 合同验证——基 2/基 3 手锚+分层均匀
 * 圣像+值域+确定性+fail-fast。
 */
class VanDerCorputTest {

    @Test
    void shouldMatchHandAnchor_whenBaseTwo() {
        double[] points = VanDerCorput.sequence(2, 8);
        double[] expected = {0, 0.5, 0.25, 0.75, 0.125, 0.625, 0.375, 0.875};
        assertThat(points).containsExactly(expected);
    }

    @Test
    void shouldMatchHandAnchor_whenBaseThree() {
        double[] points = VanDerCorput.sequence(3, 4);
        assertThat(points[0]).isCloseTo(0.0, within(1e-12));
        assertThat(points[1]).isCloseTo(1.0 / 3, within(1e-12));
        assertThat(points[2]).isCloseTo(2.0 / 3, within(1e-12));
        assertThat(points[3]).isCloseTo(1.0 / 9, within(1e-12));
    }

    @Test
    void shouldStratifyOnePointPerBin_whenBaseTwoEightPoints() {
        double[] points = VanDerCorput.sequence(2, 8);
        int[] bins = new int[8];
        for (double p : points) {
            bins[(int) (p * 8)]++;
        }
        assertThat(bins).containsOnly(1);
    }

    @Test
    void shouldStayInUnitInterval_whenBaseTenLongRun() {
        double[] points = VanDerCorput.sequence(10, 1000);
        for (double p : points) {
            assertThat(p).isBetween(0.0, 1.0);
        }
    }

    @Test
    void shouldReproduceIdenticalSequence_whenSameInputTwice() {
        assertThat(VanDerCorput.sequence(3, 50)).isEqualTo(VanDerCorput.sequence(3, 50));
    }

    @Test
    void shouldFailFast_whenBaseOrCountInvalid() {
        assertThatThrownBy(() -> VanDerCorput.sequence(1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("基 ≥2");
        assertThatThrownBy(() -> VanDerCorput.sequence(2, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非负");
    }
}
