package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1854 / T2910：双窗漂移——双闸显著、方向分开、零基线退化。 */
class WindowShiftDetectorTest {

    /** 升漂与降漂方向分开：绝对与相对双闸同过。 */
    @Test
    void shouldDetectDirectionalShifts() {
        assertThat(WindowShiftDetector.detect(
                List.of(100d, 102d, 98d), List.of(150d, 152d, 148d), 20d, 0.2d))
                .isEqualTo(WindowShiftDetector.Shift.SHIFTED_UP);
        assertThat(WindowShiftDetector.detect(
                List.of(150d, 152d, 148d), List.of(100d, 102d, 98d), 20d, 0.2d))
                .isEqualTo(WindowShiftDetector.Shift.SHIFTED_DOWN);
    }

    /** 双闸缺一不可：绝对过相对不过（小基线大波动）→ STABLE。 */
    @Test
    void bothGatesMustPass() {
        // 基线 1.0 近窗 3.0：绝对差 2 ≥ 1 过，相对 2/1=2 ≥ 0.5 过 → 漂
        assertThat(WindowShiftDetector.detect(
                List.of(1d), List.of(3d), 1d, 0.5d))
                .isEqualTo(WindowShiftDetector.Shift.SHIFTED_UP);
        // 基线 1.0 近窗 1.9：绝对 0.9 < 1 不过 → STABLE
        assertThat(WindowShiftDetector.detect(
                List.of(1d), List.of(1.9d), 1d, 0.5d))
                .isEqualTo(WindowShiftDetector.Shift.STABLE);
    }

    /** 零基线退化：相对分母 max(|0|,1)=1——纯绝对口径。 */
    @Test
    void zeroBaselineDegradesToAbsolute() {
        // 均值 0→6：绝对 6≥4 过；相对 6 ≥ 5×1 过 → 漂（分母退化为 1）
        assertThat(WindowShiftDetector.detect(
                List.of(0d, 0d), List.of(5d, 7d), 4d, 5d))
                .isEqualTo(WindowShiftDetector.Shift.SHIFTED_UP);
    }

    /** 畸形入参 fail-fast：空窗、负闸参、null/NaN 样本。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> WindowShiftDetector.detect(
                List.of(), List.of(1d), 1d, 0.5d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("两窗均须非空");
        assertThatThrownBy(() -> WindowShiftDetector.detect(
                List.of(1d), List.of(1d), -1d, 0.5d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("闸参非法");
        assertThatThrownBy(() -> WindowShiftDetector.detect(
                List.of(1d), java.util.Arrays.asList(1d, null), 1d, 0.5d))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WindowShiftDetector.detect(
                List.of(Double.NaN), List.of(1d), 1d, 0.5d))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
