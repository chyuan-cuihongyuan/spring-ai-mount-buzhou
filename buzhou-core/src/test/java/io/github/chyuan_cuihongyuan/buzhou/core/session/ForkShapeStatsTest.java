package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 1706 / T2614：ForkShapeStats 纯函数直测——深度/宽度/叶子/根与环拒绝。
 */
class ForkShapeStatsTest {

    @Test
    void emptyMapCarriesSentinel() {
        var report = ForkShapeStats.analyze(Map.of());
        assertThat(report.totalSessions()).isZero();
        assertThat(report.maxDepth()).isEqualTo(-1);
        assertThat(ForkShapeStats.analyze(null).totalSessions()).isZero();
    }

    @Test
    void singleRootChainDepth() {
        // r -> a -> b：深度 2，宽 1，叶 b
        Map<String, String> tree = new HashMap<>();
        tree.put("r", null);
        tree.put("a", "r");
        tree.put("b", "a");
        var report = ForkShapeStats.analyze(tree);
        assertThat(report.totalSessions()).isEqualTo(3);
        assertThat(report.rootCount()).isEqualTo(1);
        assertThat(report.forkCount()).isEqualTo(2);
        assertThat(report.maxDepth()).isEqualTo(2);
        assertThat(report.maxOutdegree()).isEqualTo(1);
        assertThat(report.leafCount()).isEqualTo(1);
    }

    @Test
    void wideFanOutAndMultipleLeaves() {
        Map<String, String> tree = new HashMap<>();
        tree.put("hub", null);
        tree.put("a", "hub");
        tree.put("b", "hub");
        tree.put("c", "hub");
        tree.put("d", "a");
        var report = ForkShapeStats.analyze(tree);
        assertThat(report.maxOutdegree()).isEqualTo(3);
        assertThat(report.leafCount()).isEqualTo(3);
        assertThat(report.maxDepth()).isEqualTo(2);
        assertThat(report.rootCount()).isEqualTo(1);
    }

    @Test
    void forestCountsMultipleRoots() {
        Map<String, String> tree = new HashMap<>();
        tree.put("r1", null);
        tree.put("r2", null);
        tree.put("c", "r1");
        var report = ForkShapeStats.analyze(tree);
        assertThat(report.rootCount()).isEqualTo(2);
        assertThat(report.forkCount()).isEqualTo(1);
    }

    @Test
    void cycleIsRejectedHonestly() {
        Map<String, String> cyclic = new HashMap<>();
        cyclic.put("a", "b");
        cyclic.put("b", "a");
        assertThatThrownBy(() -> ForkShapeStats.analyze(cyclic))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
