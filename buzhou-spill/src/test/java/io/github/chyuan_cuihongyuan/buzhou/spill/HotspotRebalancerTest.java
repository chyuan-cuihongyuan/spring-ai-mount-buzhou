package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1814 / T2830：热点重平衡——贪心搬迁、容差停手、不越衡反转。 */
class HotspotRebalancerTest {

    /** 两节点越容差：贪心搬最热到最冷，落容差内停手。 */
    @Test
    void shouldRebalanceIntoTolerance() {
        HotspotRebalancer.RebalancePlan plan = HotspotRebalancer.suggest(3, 2, List.of(
                new HotspotRebalancer.NodeLoad("hot", 10),
                new HotspotRebalancer.NodeLoad("cold", 0)));
        assertThat(plan.spreadBefore()).isEqualTo(10L);
        assertThat(plan.spreadAfter()).isLessThanOrEqualTo(2L);
        assertThat(plan.moves()).isNotEmpty();
        assertThat(plan.moves().get(0).from()).isEqualTo("hot");
        assertThat(plan.moves().get(0).to()).isEqualTo("cold");
        assertThat(plan.improvementRatio()).isGreaterThan(0.5d);
    }

    /** 量子不越衡反转：单步量受 spread/2 封顶，不把最热搬成最冷。 */
    @Test
    void quantumNeverInvertsBalance() {
        HotspotRebalancer.RebalancePlan plan = HotspotRebalancer.suggest(100, 0, List.of(
                new HotspotRebalancer.NodeLoad("a", 5),
                new HotspotRebalancer.NodeLoad("b", 0)));
        assertThat(plan.moves()).hasSize(1);
        assertThat(plan.moves().get(0).quantity()).isEqualTo(2L);
        assertThat(plan.spreadAfter()).isEqualTo(1L);
    }

    /** 容差内不动：已均衡输入零建议。 */
    @Test
    void balancedInputYieldsNoMoves() {
        HotspotRebalancer.RebalancePlan plan = HotspotRebalancer.suggest(5, 4, List.of(
                new HotspotRebalancer.NodeLoad("a", 10),
                new HotspotRebalancer.NodeLoad("b", 7)));
        assertThat(plan.moves()).isEmpty();
        assertThat(plan.spreadAfter()).isEqualTo(3L);
        assertThat(plan.improvementRatio()).isZero();
    }

    /** 少于两节点与空表/null：空计划哨兵。 */
    @Test
    void fewerThanTwoNodesYieldEmptyPlan() {
        List<List<HotspotRebalancer.NodeLoad>> cases =
                java.util.Arrays.<List<HotspotRebalancer.NodeLoad>>asList(
                        List.of(),
                        null,
                        List.of(new HotspotRebalancer.NodeLoad("solo", 5)));
        for (List<HotspotRebalancer.NodeLoad> loads : cases) {
            HotspotRebalancer.RebalancePlan plan = HotspotRebalancer.suggest(1, 0, loads);
            assertThat(plan.moves()).isEmpty();
            assertThat(plan.improvementRatio()).isEqualTo(-1d);
        }
    }

    /** 畸形入参 fail-fast：量子 < 1、负容差、空白 id、负负载。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> HotspotRebalancer.suggest(0, 0, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moveQuantum 不能小于 1");
        assertThatThrownBy(() -> HotspotRebalancer.suggest(1, -1, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HotspotRebalancer.NodeLoad("", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HotspotRebalancer.NodeLoad("a", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 确定性：并列极值取 id 字典序，同入参同出参。 */
    @Test
    void deterministicOnTies() {
        List<HotspotRebalancer.NodeLoad> loads = List.of(
                new HotspotRebalancer.NodeLoad("b", 9),
                new HotspotRebalancer.NodeLoad("a", 9),
                new HotspotRebalancer.NodeLoad("c", 0));
        HotspotRebalancer.RebalancePlan first = HotspotRebalancer.suggest(2, 0, loads);
        HotspotRebalancer.RebalancePlan second = HotspotRebalancer.suggest(2, 0, loads);
        assertThat(second).isEqualTo(first);
        assertThat(first.moves().get(0).from()).isEqualTo("a");
        assertThat(first.moves().get(0).to()).isEqualTo("c");
    }
}
