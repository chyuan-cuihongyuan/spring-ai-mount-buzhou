package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1909 / T3020：闲谈收敛——轮数、知情数、fanout 反解、畸形。 */
class GossipConvergenceTest {

    /** 经典：1024 节点 fanout 3 → log_4(1024) = 5 轮。 */
    @Test
    void classicConvergence() {
        assertThat(GossipConvergence.roundsToConverge(1024, 3)).isEqualTo(5);
        assertThat(GossipConvergence.informedAfter(1024, 3, 5)).isEqualTo(1024);
    }

    /** 知情数封顶 N：轮数再多不超节点总数。 */
    @Test
    void informedCappedAtNodes() {
        assertThat(GossipConvergence.informedAfter(100, 3, 10)).isEqualTo(100);
        assertThat(GossipConvergence.informedAfter(100, 3, 1)).isEqualTo(4);
    }

    /** fanout 反解：1000 节点 10 轮 → fanout 1 即达（2^10=1024≥1000）；8 轮则需 2。 */
    @Test
    void fanoutInverseAndSingleNode() {
        assertThat(GossipConvergence.fanoutFor(1000, 10)).isEqualTo(1);
        assertThat(GossipConvergence.fanoutFor(1000, 8)).isEqualTo(2);
        assertThat(GossipConvergence.informedAfter(1, 1, 0)).isEqualTo(1);
    }

    /** 畸形入参 fail-fast：nodes=0、fanout=0、负轮数。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> GossipConvergence.roundsToConverge(0, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nodes 不能小于 1");
        assertThatThrownBy(() -> GossipConvergence.roundsToConverge(100, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fanout 不能小于 1");
        assertThatThrownBy(() -> GossipConvergence.informedAfter(100, 3, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rounds 不能为负");
    }
}
