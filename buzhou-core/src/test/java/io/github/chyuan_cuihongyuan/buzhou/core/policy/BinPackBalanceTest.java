package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1878 / T2958：FFD 装箱——经典装、完美装、空表、浪费率、畸形。 */
class BinPackBalanceTest {

    /** 经典 [4,3,3,2,2]/6：FFD → 3 箱载荷 {6,6,2}。 */
    @Test
    void classicPackUsesThreeBins() {
        BinPackBalance.PackResult r =
                BinPackBalance.pack(new long[]{4, 3, 3, 2, 2}, 6);
        assertThat(r.binsUsed()).isEqualTo(3);
        assertThat(r.loads()).containsExactly(6L, 6L, 2L);
    }

    /** 完美装 [3,3,3]/9 → 1 箱 0 浪费；单件恰容量同样 0 浪费。 */
    @Test
    void perfectFitWastesNothing() {
        BinPackBalance.PackResult r = BinPackBalance.pack(new long[]{3, 3, 3}, 9);
        assertThat(r.binsUsed()).isEqualTo(1);
        assertThat(BinPackBalance.wasteRatio(r.loads(), 9)).isZero();
        BinPackBalance.PackResult single = BinPackBalance.pack(new long[]{7}, 7);
        assertThat(single.binsUsed()).isEqualTo(1);
        assertThat(BinPackBalance.wasteRatio(single.loads(), 7)).isZero();
    }

    /** 空表 → 0 箱哨兵，浪费率哨兵 0.0。 */
    @Test
    void emptyItemsPackToZeroBins() {
        BinPackBalance.PackResult r = BinPackBalance.pack(new long[0], 5);
        assertThat(r.binsUsed()).isZero();
        assertThat(r.loads()).isEmpty();
        assertThat(BinPackBalance.wasteRatio(new long[0], 5)).isZero();
    }

    /** 浪费率：载荷 {6,6,2} 容量 6 → 1 − 14/18 = 2/9。 */
    @Test
    void wasteRatioPrecise() {
        assertThat(BinPackBalance.wasteRatio(new long[]{6, 6, 2}, 6))
                .isCloseTo(2.0 / 9.0, within(1e-12));
    }

    /** 畸形入参 fail-fast：容量 0、负体积、单件超容、负载荷。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> BinPackBalance.pack(new long[]{1}, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("binCapacity 不能小于 1");
        assertThatThrownBy(() -> BinPackBalance.pack(new long[]{-1}, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("体积不能为负");
        assertThatThrownBy(() -> BinPackBalance.pack(new long[]{6}, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("单件体积超过箱容量");
        assertThatThrownBy(() -> BinPackBalance.wasteRatio(new long[]{-2}, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("载荷不能为负");
    }
}
