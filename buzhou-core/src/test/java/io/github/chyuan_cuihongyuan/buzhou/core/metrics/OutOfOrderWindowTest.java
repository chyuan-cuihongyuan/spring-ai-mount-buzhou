package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1899 / T3000：乱序接纳窗——三态、窗沿、水位推进、畸形。 */
class OutOfOrderWindowTest {

    /** 三态：水位 1000 窗 200 → 1100 FRESH / 850 LATE / 700 TOO_OLD。 */
    @Test
    void threeWayClassification() {
        assertThat(OutOfOrderWindow.classify(1100, 1000, 200))
                .isEqualTo(OutOfOrderWindow.Accept.FRESH);
        assertThat(OutOfOrderWindow.classify(850, 1000, 200))
                .isEqualTo(OutOfOrderWindow.Accept.LATE_ACCEPTED);
        assertThat(OutOfOrderWindow.classify(700, 1000, 200))
                .isEqualTo(OutOfOrderWindow.Accept.TOO_OLD);
    }

    /** 窗沿含下：恰 800（= 1000−200）LATE；恰 1000 FRESH。 */
    @Test
    void windowEdgesInclusive() {
        assertThat(OutOfOrderWindow.classify(800, 1000, 200))
                .isEqualTo(OutOfOrderWindow.Accept.LATE_ACCEPTED);
        assertThat(OutOfOrderWindow.classify(1000, 1000, 200))
                .isEqualTo(OutOfOrderWindow.Accept.FRESH);
    }

    /** 水位推进取大不回退；迟到事件不动水位。 */
    @Test
    void advanceMonotonic() {
        assertThat(OutOfOrderWindow.advance(1000, 1200)).isEqualTo(1200);
        assertThat(OutOfOrderWindow.advance(1000, 800)).isEqualTo(1000);
        assertThat(OutOfOrderWindow.advance(1000, 1000)).isEqualTo(1000);
    }

    /** 畸形入参 fail-fast：负窗、负时点。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> OutOfOrderWindow.classify(100, 1000, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("window 不能为负");
        assertThatThrownBy(() -> OutOfOrderWindow.classify(-5, 1000, 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("时点不能为负");
        assertThatThrownBy(() -> OutOfOrderWindow.advance(1000, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventTime 不能为负");
    }
}
