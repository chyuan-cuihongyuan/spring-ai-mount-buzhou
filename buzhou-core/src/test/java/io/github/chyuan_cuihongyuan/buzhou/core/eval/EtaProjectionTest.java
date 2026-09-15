package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1834 / T2870：ETA 投影——线性外推、哨兵、边界。 */
class EtaProjectionTest {

    /** 线性外推：10/40 用 100ms → ETA 300ms、投影总 400ms。 */
    @Test
    void shouldProjectLinearly() {
        EtaProjection.Projection p = EtaProjection.estimate(10, 40, 100L);
        assertThat(p.progress()).isEqualTo(0.25d);
        assertThat(p.etaMillis()).isEqualTo(300L);
        assertThat(p.projectedTotalMillis()).isEqualTo(400L);
    }

    /** 无速率基准哨兵：done=0 或 elapsed=0 → ETA/投影 -1（不编速率）。 */
    @Test
    void noRateBasisYieldsSentinels() {
        assertThat(EtaProjection.estimate(0, 40, 100L).etaMillis()).isEqualTo(-1L);
        assertThat(EtaProjection.estimate(5, 40, 0L).etaMillis()).isEqualTo(-1L);
        assertThat(EtaProjection.estimate(0, 40, 100L).progress()).isZero();
    }

    /** 已完成：ETA 0、投影=elapsed。 */
    @Test
    void completedYieldsZeroEta() {
        EtaProjection.Projection p = EtaProjection.estimate(40, 40, 250L);
        assertThat(p.progress()).isEqualTo(1.0d);
        assertThat(p.etaMillis()).isZero();
        assertThat(p.projectedTotalMillis()).isEqualTo(250L);
    }

    /** 除不尽向上取整（保守——宁可多等不可少报）。 */
    @Test
    void roundsUpRemainder() {
        EtaProjection.Projection p = EtaProjection.estimate(3, 10, 100L);
        // 剩 7 单位 × (100/3) = 233.33 → 234
        assertThat(p.etaMillis()).isEqualTo(234L);
    }

    /** 畸形入参 fail-fast：total<1、done 越界、负 elapsed。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> EtaProjection.estimate(5, 0, 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalUnits 不能小于 1");
        assertThatThrownBy(() -> EtaProjection.estimate(11, 10, 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("doneUnits 越界");
        assertThatThrownBy(() -> EtaProjection.estimate(5, 10, -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("elapsedMillis 不能为负");
    }
}
