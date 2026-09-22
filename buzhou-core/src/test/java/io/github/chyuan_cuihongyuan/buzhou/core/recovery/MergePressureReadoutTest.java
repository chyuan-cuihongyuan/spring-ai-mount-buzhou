package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1900 / T3002：合并压力——占比、三态、硬阀、畸形。 */
class MergePressureReadoutTest {

    /** 占比读数：活跃 150/建议 300 = 0.5。 */
    @Test
    void pressureRatio() {
        assertThat(MergePressureReadout.pressure(150, 300))
                .isCloseTo(0.5, within(1e-12));
    }

    /** 三态与边界含上：0.4 OK、恰 0.5 WARN、恰 1.0 REJECT。 */
    @Test
    void verdictBandsInclusive() {
        assertThat(MergePressureReadout.verdict(0.4, 0.5))
                .isEqualTo(MergePressureReadout.Pressure.OK);
        assertThat(MergePressureReadout.verdict(0.5, 0.5))
                .isEqualTo(MergePressureReadout.Pressure.WARN);
        assertThat(MergePressureReadout.verdict(1.0, 0.5))
                .isEqualTo(MergePressureReadout.Pressure.REJECT);
    }

    /** 硬阀独立：350 < 400 不拒、恰 400 拒（含上）。 */
    @Test
    void hardValveIndependent() {
        assertThat(MergePressureReadout.shouldRejectInsert(350, 400)).isFalse();
        assertThat(MergePressureReadout.shouldRejectInsert(400, 400)).isTrue();
    }

    /** 畸形入参 fail-fast：负活跃数、零建议上限、warnAt 越界、硬阀 0。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> MergePressureReadout.pressure(-1, 300))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("activeParts 不能为负");
        assertThatThrownBy(() -> MergePressureReadout.pressure(10, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recommendedMax 不能小于 1");
        assertThatThrownBy(() -> MergePressureReadout.verdict(0.5, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("warnAt 须在 (0,1]");
        assertThatThrownBy(() -> MergePressureReadout.shouldRejectInsert(10, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hardMax 不能小于 1");
    }
}
