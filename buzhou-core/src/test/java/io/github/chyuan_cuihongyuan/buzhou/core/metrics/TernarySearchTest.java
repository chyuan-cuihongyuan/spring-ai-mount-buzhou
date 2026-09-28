package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.function.DoubleUnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7042：TernarySearch 合同——单峰三分极值。手锚抛物线
 * 极值；容差收敛；极小对称面；fail-fast。
 */
class TernarySearchTest {

    @Test
    void parabolaMaximumAnchors() {
        DoubleUnaryOperator parabola = x -> -(x - 3) * (x - 3) + 5;
        double peak = TernarySearch.maximize(parabola, 0, 10, 1e-7);
        assertThat(peak).as("抛物线顶点 x=3").isBetween(2.999, 3.001);
        assertThat(parabola.applyAsDouble(peak)).isBetween(4.999999, 5.000001);
    }

    @Test
    void minimumMirrorFace() {
        DoubleUnaryOperator valley = x -> (x + 2) * (x + 2) - 7;
        double bottom = TernarySearch.minimize(valley, -20, 20, 1e-7);
        assertThat(bottom).as("谷底 x=-2").isBetween(-2.001, -1.999);
        assertThat(valley.applyAsDouble(bottom)).isBetween(-7.000001, -6.999999);
    }

    @Test
    void tightEpsilonRefines() {
        DoubleUnaryOperator parabola = x -> -(x - 1.234) * (x - 1.234);
        double coarse = TernarySearch.maximize(parabola, 0, 5, 1e-3);
        double fine = TernarySearch.maximize(parabola, 0, 5, 1e-9);
        assertThat(Math.abs(fine - 1.234)).isLessThan(Math.abs(coarse - 1.234));
        assertThat(Math.abs(fine - 1.234)).isLessThan(1e-8);
    }

    @Test
    void failFastContract() {
        DoubleUnaryOperator f = x -> x;
        assertThatThrownBy(() -> TernarySearch.maximize(null, 0, 1, 1e-6))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TernarySearch.maximize(f, 5, 1, 1e-6))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TernarySearch.maximize(f, 0, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TernarySearch.minimize(f, 0, 1, -1e-6))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
