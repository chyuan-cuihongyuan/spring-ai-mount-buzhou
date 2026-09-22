package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** spec 1881 / T2964：纠删码预算——EC(4,2) 经典、副本对照、畸形。 */
class ErasureCodingBudgetTest {

    /** EC(4,2) MinIO 默认形：可用率 2/3、可丢 2、修复读 4、部署 6 盘。 */
    @Test
    void minioDefaultFourTwo() {
        assertThat(ErasureCodingBudget.usableRatio(4, 2)).isCloseTo(2.0 / 3, within(1e-12));
        assertThat(ErasureCodingBudget.tolerableFailures(4, 2)).isEqualTo(2);
        assertThat(ErasureCodingBudget.repairReads(4)).isEqualTo(4);
        assertThat(ErasureCodingBudget.totalShards(4, 2)).isEqualTo(6);
    }

    /** EC(8,4)：同可用率 2/3，修复读翻倍 8——重建窗口网络压力口径。 */
    @Test
    void eightFourScalesRepairReads() {
        assertThat(ErasureCodingBudget.usableRatio(8, 4)).isCloseTo(2.0 / 3, within(1e-12));
        assertThat(ErasureCodingBudget.repairReads(8)).isEqualTo(8);
        assertThat(ErasureCodingBudget.tolerableFailures(8, 4)).isEqualTo(4);
    }

    /** 副本对照：三副本 = EC(1,2)——同容忍 2 片，可用率仅 1/3。 */
    @Test
    void threeReplicasComparison() {
        assertThat(ErasureCodingBudget.usableRatio(1, 2)).isCloseTo(1.0 / 3, within(1e-12));
        assertThat(ErasureCodingBudget.tolerableFailures(1, 2)).isEqualTo(2);
        assertThat(ErasureCodingBudget.repairReads(1)).isEqualTo(1);
    }

    /** 畸形入参 fail-fast：data=0、parity=0（零冗余）、负值。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> ErasureCodingBudget.usableRatio(0, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data 不能小于 1");
        assertThatThrownBy(() -> ErasureCodingBudget.usableRatio(4, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("零冗余丢一片即丢数据");
        assertThatThrownBy(() -> ErasureCodingBudget.repairReads(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data 不能小于 1");
    }
}
