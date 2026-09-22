package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1904 / T3010：票数下限——多数派、拜占庭容忍、最小规模、畸形。 */
class QuorumThresholdTest {

    /** 简单多数：3/4/5 投票者 → 2/3/3。 */
    @Test
    void majorityThresholds() {
        assertThat(QuorumThreshold.majority(3)).isEqualTo(2);
        assertThat(QuorumThreshold.majority(4)).isEqualTo(3);
        assertThat(QuorumThreshold.majority(5)).isEqualTo(3);
    }

    /** 拜占庭容忍：3/4/7 投票者 → 0/1/2 坏票。 */
    @Test
    void byzantineToleranceScales() {
        assertThat(QuorumThreshold.byzantineTolerance(3)).isZero();
        assertThat(QuorumThreshold.byzantineTolerance(4)).isEqualTo(1);
        assertThat(QuorumThreshold.byzantineTolerance(7)).isEqualTo(2);
    }

    /** 最小规模：容 1 坏票需 4、容 2 需 7、容 0 需 1。 */
    @Test
    void minimumByzantineSize() {
        assertThat(QuorumThreshold.byzantineSize(1)).isEqualTo(4);
        assertThat(QuorumThreshold.byzantineSize(2)).isEqualTo(7);
        assertThat(QuorumThreshold.byzantineSize(0)).isEqualTo(1);
    }

    /** 畸形入参 fail-fast：voters=0、负 faults。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> QuorumThreshold.majority(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("voters 不能小于 1");
        assertThatThrownBy(() -> QuorumThreshold.byzantineTolerance(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("voters 不能小于 1");
        assertThatThrownBy(() -> QuorumThreshold.byzantineSize(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("faults 不能为负");
    }
}
