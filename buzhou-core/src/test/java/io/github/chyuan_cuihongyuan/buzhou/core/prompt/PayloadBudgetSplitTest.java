package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1897 / T2996：载荷预算分账——水填、宽松、权重、畸形。 */
class PayloadBudgetSplitTest {

    /** 经典水填：{30,50,70} 预算 100 等权 → {30,35,35}，小需求完整。 */
    @Test
    void classicWaterFilling() {
        PayloadBudgetSplit.Allocation a =
                PayloadBudgetSplit.allocate(100, new long[]{30, 50, 70},
                        new long[]{1, 1, 1});
        assertThat(a.grants()).containsExactly(30L, 35L, 35L);
        assertThat(a.unmetDemand()).isEqualTo(50L);
    }

    /** 宽松：总需求 ≤ 预算 → 全额授予零浪费。 */
    @Test
    void looseBudgetGrantsInFull() {
        PayloadBudgetSplit.Allocation a =
                PayloadBudgetSplit.allocate(1000, new long[]{30, 50, 70},
                        new long[]{1, 1, 1});
        assertThat(a.grants()).containsExactly(30L, 50L, 70L);
        assertThat(a.unmetDemand()).isZero();
    }

    /** 权重倾斜 3:1：预算 40 需求 {40,40} → {30,10}——权重直接映射份额。 */
    @Test
    void weightTiltsShare() {
        PayloadBudgetSplit.Allocation a =
                PayloadBudgetSplit.allocate(40, new long[]{40, 40},
                        new long[]{3, 1});
        assertThat(a.grants()[0]).isEqualTo(30L);
        assertThat(a.grants()[1]).isEqualTo(10L);
        assertThat(a.unmetDemand()).isEqualTo(40L);
    }

    /** 畸形入参 fail-fast：负预算、不等长、零权重、负需求。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> PayloadBudgetSplit.allocate(-1,
                        new long[]{1}, new long[]{1}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("budget 不能为负");
        assertThatThrownBy(() -> PayloadBudgetSplit.allocate(10,
                        new long[]{1, 2}, new long[]{1}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("同长且非空");
        assertThatThrownBy(() -> PayloadBudgetSplit.allocate(10,
                        new long[]{1}, new long[]{0}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("权重不能小于 1");
        assertThatThrownBy(() -> PayloadBudgetSplit.allocate(10,
                        new long[]{-1}, new long[]{1}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("需求不能为负");
    }
}
