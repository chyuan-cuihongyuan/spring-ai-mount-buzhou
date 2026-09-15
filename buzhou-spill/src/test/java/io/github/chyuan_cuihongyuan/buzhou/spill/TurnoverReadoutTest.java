package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1836 / T2874：库存周转——周转次数、耗尽视界、哨兵与保守取整。 */
class TurnoverReadoutTest {

    /** 周转次数：消费 300 / 库存 100 = 3 轮（周期内库存转三圈）。 */
    @Test
    void shouldComputeTurns() {
        assertThat(TurnoverReadout.turns(100, 300)).isEqualTo(3.0d);
        assertThat(TurnoverReadout.turns(100, 0)).isZero();
        assertThat(TurnoverReadout.turns(0, 100)).isEqualTo(-1d);
    }

    /** 耗尽视界：库存 100 / 速率 0.4/ms = 250ms；取整保守向上。 */
    @Test
    void shouldComputeDepletionHorizon() {
        assertThat(TurnoverReadout.depletionHorizonMillis(100, 0.4d)).isEqualTo(250L);
        assertThat(TurnoverReadout.depletionHorizonMillis(101, 0.4d)).isEqualTo(253L);
        assertThat(TurnoverReadout.depletionHorizonMillis(0, 1.0d)).isZero();
        assertThat(TurnoverReadout.depletionHorizonMillis(100, 0.0d)).isEqualTo(-1L);
    }

    /** 畸形入参 fail-fast：负库存/负消费/NaN 速率。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> TurnoverReadout.turns(-1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("入参不能为负");
        assertThatThrownBy(() -> TurnoverReadout.turns(10, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TurnoverReadout.depletionHorizonMillis(-1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TurnoverReadout.depletionHorizonMillis(10, Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TurnoverReadout.depletionHorizonMillis(10, -0.1d))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
