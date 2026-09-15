package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1838 / T2878：轮换重叠窗——三态判、边界含、未来代拒绝、普查。 */
class RotationOverlapWindowTest {

    /** 三态判定：同代 CURRENT、窗内 GRACE（含边界）、窗尽 EXPIRED。 */
    @Test
    void shouldJudgeThreeStatesWithInclusiveGraceBoundary() {
        assertThat(RotationOverlapWindow.validity(10, 10, 2))
                .isEqualTo(RotationOverlapWindow.EpochValidity.CURRENT);
        assertThat(RotationOverlapWindow.validity(9, 10, 2))
                .isEqualTo(RotationOverlapWindow.EpochValidity.GRACE);
        assertThat(RotationOverlapWindow.validity(8, 10, 2))
                .isEqualTo(RotationOverlapWindow.EpochValidity.GRACE);
        assertThat(RotationOverlapWindow.validity(7, 10, 2))
                .isEqualTo(RotationOverlapWindow.EpochValidity.EXPIRED);
    }

    /** 零宽窗退化为硬切换（无重叠——蓝绿不接的对照面）。 */
    @Test
    void zeroGraceDegradesToHardCutover() {
        assertThat(RotationOverlapWindow.validity(10, 10, 0))
                .isEqualTo(RotationOverlapWindow.EpochValidity.CURRENT);
        assertThat(RotationOverlapWindow.validity(9, 10, 0))
                .isEqualTo(RotationOverlapWindow.EpochValidity.EXPIRED);
    }

    /** 普查：三态计数 + 失效占比（清扫进度）。 */
    @Test
    void censusCountsAndExpiredRatio() {
        RotationOverlapWindow.Census census = RotationOverlapWindow.census(10, 2,
                List.of(10L, 10L, 9L, 8L, 7L));
        assertThat(census.credentials()).isEqualTo(5);
        assertThat(census.current()).isEqualTo(2);
        assertThat(census.grace()).isEqualTo(2);
        assertThat(census.expired()).isEqualTo(1);
        assertThat(census.expiredRatio()).isEqualTo(0.2d);
        assertThat(RotationOverlapWindow.census(10, 2, null).expiredRatio())
                .isEqualTo(-1d);
    }

    /** 畸形入参 fail-fast：负 epoch/宽、未来代、null 凭据。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> RotationOverlapWindow.validity(-1, 10, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RotationOverlapWindow.validity(10, 10, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RotationOverlapWindow.validity(11, 10, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未来代凭据");
        assertThatThrownBy(() -> RotationOverlapWindow.census(10, 2,
                java.util.Arrays.asList(10L, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
