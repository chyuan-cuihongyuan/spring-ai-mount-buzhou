package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1888 / T2978：连接池启发——公式、拆分、饱和度、畸形。 */
class PoolSizeHeuristicTest {

    /** 经典 HikariCP 公式：4 核 + 2 磁轴 → 10。 */
    @Test
    void classicFormula() {
        assertThat(PoolSizeHeuristic.optimalSize(4, 2)).isEqualTo(10);
        assertThat(PoolSizeHeuristic.optimalSize(8, 4)).isEqualTo(20);
    }

    /** SSD 形态：磁轴 0 → 纯 2×cores。 */
    @Test
    void ssdZeroSpindles() {
        assertThat(PoolSizeHeuristic.optimalSize(4, 0)).isEqualTo(8);
    }

    /** 预算拆分：23 拆 4 节点 → {6,6,6,5}——余数摊平不偏科。 */
    @Test
    void budgetSpreadEvenly() {
        assertThat(PoolSizeHeuristic.splitBudget(23, 4)).containsExactly(6, 6, 6, 5);
        assertThat(PoolSizeHeuristic.splitBudget(20, 4)).containsExactly(5, 5, 5, 5);
    }

    /** 饱和度：19/20=0.95；零池哨兵 0.0。 */
    @Test
    void saturationReadout() {
        assertThat(PoolSizeHeuristic.saturationRatio(19, 20)).isCloseTo(0.95, within(1e-12));
        assertThat(PoolSizeHeuristic.saturationRatio(0, 0)).isCloseTo(0.0, within(1e-12));
    }

    /** 畸形入参 fail-fast：cores=0、负磁轴、预算 < 节点、负活跃数。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> PoolSizeHeuristic.optimalSize(0, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cores 不能小于 1");
        assertThatThrownBy(() -> PoolSizeHeuristic.optimalSize(4, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("spindles 不能为负");
        assertThatThrownBy(() -> PoolSizeHeuristic.splitBudget(3, 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("total 须 ≥ nodes");
        assertThatThrownBy(() -> PoolSizeHeuristic.saturationRatio(-1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("active 不能为负");
    }
}
