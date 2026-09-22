package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1886 / T2974：时钟偏斜钳位——平移、内嵌、收缩、串联、畸形。 */
class ClockSkewClampTest {

    /** 负偏斜平移：子 1000-1100 / 父 1050-1200 → 1050-1150（时长 100 保持）。 */
    @Test
    void negativeSkewShiftsPreservingDuration() {
        ClockSkewClamp.ClampedSpan r =
                ClockSkewClamp.clamp(1000, 1100, 1050, 1200);
        assertThat(r.begin()).isEqualTo(1050);
        assertThat(r.end()).isEqualTo(1150);
        assertThat(r.skewApplied()).isEqualTo(50);
        assertThat(ClockSkewClamp.skewMillis(1000, 1050)).isEqualTo(-50);
    }

    /** 正常内嵌：零改动零偏斜。 */
    @Test
    void nestedSpanUntouched() {
        ClockSkewClamp.ClampedSpan r =
                ClockSkewClamp.clamp(1010, 1090, 1000, 1200);
        assertThat(r.begin()).isEqualTo(1010);
        assertThat(r.end()).isEqualTo(1090);
        assertThat(r.skewApplied()).isZero();
    }

    /** 终点越界收缩：子 1100-1300 / 父 1000-1200 → 1100-1200。 */
    @Test
    void trailingOverflowShrinksDuration() {
        ClockSkewClamp.ClampedSpan r =
                ClockSkewClamp.clamp(1100, 1300, 1000, 1200);
        assertThat(r.begin()).isEqualTo(1100);
        assertThat(r.end()).isEqualTo(1200);
        assertThat(r.skewApplied()).isZero();
    }

    /** 双越界串联：子 900-1050 / 父 1000-1100 → 先平移后收缩 → 1000-1100。 */
    @Test
    void bothSidesClampChained() {
        ClockSkewClamp.ClampedSpan r =
                ClockSkewClamp.clamp(900, 1050, 1000, 1100);
        assertThat(r.begin()).isEqualTo(1000);
        assertThat(r.end()).isEqualTo(1100);
        assertThat(r.skewApplied()).isEqualTo(100);
    }

    /** 畸形入参 fail-fast：子/父区间倒置。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> ClockSkewClamp.clamp(1100, 1000, 900, 1300))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("子区间倒置");
        assertThatThrownBy(() -> ClockSkewClamp.clamp(1000, 1100, 1200, 1100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("父区间倒置");
    }
}
