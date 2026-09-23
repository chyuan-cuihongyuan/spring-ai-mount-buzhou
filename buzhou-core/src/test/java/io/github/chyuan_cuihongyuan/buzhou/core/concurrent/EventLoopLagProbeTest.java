package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1923 / T3048：事件循环滞后——滞后读数、判定、畸形。 */
class EventLoopLagProbeTest {

    /** 滞后读数：调度 0 执行 45 → 45；提前执行钳 0。 */
    @Test
    void lagReadoutClamped() {
        assertThat(EventLoopLagProbe.lagMillis(0, 45)).isEqualTo(45);
        assertThat(EventLoopLagProbe.lagMillis(100, 100)).isZero();
        assertThat(EventLoopLagProbe.lagMillis(100, 90)).isZero();
    }

    /** 判定：≤ 10 OK（恰 10 含上）、> 10 SATURATED。 */
    @Test
    void verdictBoundaryInclusive() {
        assertThat(EventLoopLagProbe.saturated(10, 10)).isFalse();
        assertThat(EventLoopLagProbe.saturated(11, 10)).isTrue();
    }

    /** 畸形入参 fail-fast：负时刻、负阈值。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> EventLoopLagProbe.lagMillis(-1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("时刻不能为负");
        assertThatThrownBy(() -> EventLoopLagProbe.saturated(5, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("threshold 不能为负");
    }
}
