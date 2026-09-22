package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1876 / T2954：法定人数——强一致判定、交集、余量、畸形 fail-fast。 */
class QuorumConsistencyTest {

    /** 经典 (3,2,2)：R+W=4>3 强一致，交集 1，读写余量各 1。 */
    @Test
    void classicThreeTwoTwoIsStrong() {
        assertThat(QuorumConsistency.strongConsistency(3, 2, 2)).isTrue();
        assertThat(QuorumConsistency.overlapCount(3, 2, 2)).isEqualTo(1);
        assertThat(QuorumConsistency.tolerableWriteFailures(3, 2)).isEqualTo(1);
        assertThat(QuorumConsistency.tolerableReadFailures(3, 2)).isEqualTo(1);
        assertThat(QuorumConsistency.consistentAvailability(3, 2, 2)).isEqualTo(1);
    }

    /** (5,3,3)：交集 1，可挂 2 副本仍凑法定人数、一致性可服务挂 2。 */
    @Test
    void fiveThreeThreeBudget() {
        assertThat(QuorumConsistency.strongConsistency(5, 3, 3)).isTrue();
        assertThat(QuorumConsistency.overlapCount(5, 3, 3)).isEqualTo(1);
        assertThat(QuorumConsistency.tolerableWriteFailures(5, 3)).isEqualTo(2);
        assertThat(QuorumConsistency.consistentAvailability(5, 3, 3)).isEqualTo(2);
    }

    /** (3,1,1)：R+W=2≤3 弱一致、交集 0——读到旧值可能发生。 */
    @Test
    void oneOneIsWeak() {
        assertThat(QuorumConsistency.strongConsistency(3, 1, 1)).isFalse();
        assertThat(QuorumConsistency.overlapCount(3, 1, 1)).isZero();
    }

    /** (4,3,2) 边界：R+W=5>4 强一致，写余 2、读余 1——一致性可用取小 1。 */
    @Test
    void asymmetricQuorumBoundary() {
        assertThat(QuorumConsistency.strongConsistency(4, 3, 2)).isTrue();
        assertThat(QuorumConsistency.overlapCount(4, 3, 2)).isEqualTo(1);
        assertThat(QuorumConsistency.tolerableWriteFailures(4, 2)).isEqualTo(2);
        assertThat(QuorumConsistency.tolerableReadFailures(4, 3)).isEqualTo(1);
        assertThat(QuorumConsistency.consistentAvailability(4, 3, 2)).isEqualTo(1);
    }

    /** 畸形入参 fail-fast：N=0、法定人数越界五型。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> QuorumConsistency.strongConsistency(0, 1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("replicas 不能小于 1");
        assertThatThrownBy(() -> QuorumConsistency.strongConsistency(3, 0, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("readQuorum");
        assertThatThrownBy(() -> QuorumConsistency.strongConsistency(3, 4, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("readQuorum");
        assertThatThrownBy(() -> QuorumConsistency.strongConsistency(3, 2, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("writeQuorum");
        assertThatThrownBy(() -> QuorumConsistency.tolerableWriteFailures(3, 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("writeQuorum");
    }
}
