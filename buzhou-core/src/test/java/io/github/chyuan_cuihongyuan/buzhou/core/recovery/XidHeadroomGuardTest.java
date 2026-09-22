package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1896 / T2994：xid 余量——四级判定、边界含上、畸形。 */
class XidHeadroomGuardTest {

    /** 四级各一例（limit 1000、warn 800、critical 950）。 */
    @Test
    void fourLevels() {
        assertThat(XidHeadroomGuard.urgency(100, 1000, 800, 950))
                .isEqualTo(XidHeadroomGuard.Urgency.OK);
        assertThat(XidHeadroomGuard.urgency(850, 1000, 800, 950))
                .isEqualTo(XidHeadroomGuard.Urgency.WARN);
        assertThat(XidHeadroomGuard.urgency(960, 1000, 800, 950))
                .isEqualTo(XidHeadroomGuard.Urgency.CRITICAL);
        assertThat(XidHeadroomGuard.urgency(1000, 1000, 800, 950))
                .isEqualTo(XidHeadroomGuard.Urgency.EXHAUSTED);
    }

    /** 边界含上：恰 warnAt 即 WARN、恰 criticalAt 即 CRITICAL。 */
    @Test
    void boundariesInclusive() {
        assertThat(XidHeadroomGuard.urgency(800, 1000, 800, 950))
                .isEqualTo(XidHeadroomGuard.Urgency.WARN);
        assertThat(XidHeadroomGuard.urgency(950, 1000, 800, 950))
                .isEqualTo(XidHeadroomGuard.Urgency.CRITICAL);
    }

    /** 余量读数：正常 50、恰耗尽 0、超发负值诚实显示。 */
    @Test
    void headroomReadout() {
        assertThat(XidHeadroomGuard.headroom(950, 1000)).isEqualTo(50);
        assertThat(XidHeadroomGuard.headroom(1000, 1000)).isZero();
        assertThat(XidHeadroomGuard.headroom(1005, 1000)).isEqualTo(-5);
    }

    /** 畸形入参 fail-fast：分级线倒置、线越 limit、消耗为负。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> XidHeadroomGuard.urgency(10, 1000, 950, 800))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("分级线须满足");
        assertThatThrownBy(() -> XidHeadroomGuard.urgency(10, 900, 800, 950))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("分级线须满足");
        assertThatThrownBy(() -> XidHeadroomGuard.urgency(-1, 1000, 800, 950))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consumed 不能为负");
    }
}
