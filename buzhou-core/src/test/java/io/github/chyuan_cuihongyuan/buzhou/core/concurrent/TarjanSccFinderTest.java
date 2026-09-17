package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3018 / T5038：Tarjan SCC 合同——双点环并组件、DAG 全单点
 * 无环、凝聚图反拓扑序（sink 先出）、自环单点算环、双环相连
 * 两组件、深链 2 万不爆栈（迭代证）、幂等重放、越界 fail-fast。
 */
class TarjanSccFinderTest {

    private static List<Integer> componentOf(List<List<Integer>> components, int vertex) {
        return components.stream().filter(c -> c.contains(vertex)).findFirst().orElseThrow();
    }

    @Test
    void twoNodeCycleShouldFormSingleComponent() {
        TarjanSccFinder finder = new TarjanSccFinder(2);
        finder.addEdge(0, 1);
        finder.addEdge(1, 0);
        List<List<Integer>> components = finder.components();
        assertThat(components).hasSize(1);
        assertThat(components.get(0)).containsExactlyInAnyOrder(0, 1);
        assertThat(finder.hasCycle()).isTrue();
        assertThat(finder.cyclicVertices()).containsExactly(0, 1);
    }

    @Test
    void dagShouldBeAllSingletonsWithoutCycle() {
        TarjanSccFinder finder = new TarjanSccFinder(4);
        finder.addEdge(0, 1);
        finder.addEdge(0, 2);
        finder.addEdge(1, 3);
        finder.addEdge(2, 3);
        List<List<Integer>> components = finder.components();
        assertThat(components).hasSize(4);
        components.forEach(c -> assertThat(c).hasSize(1));
        assertThat(finder.hasCycle()).isFalse();
        assertThat(finder.cyclicVertices()).isEmpty();
    }

    @Test
    void componentsShouldComeInReverseTopologicalOrder() {
        // 0→1、2→1：凝聚图 sink={1} 先出，再 {0}/{2}
        TarjanSccFinder finder = new TarjanSccFinder(3);
        finder.addEdge(0, 1);
        finder.addEdge(2, 1);
        List<List<Integer>> components = finder.components();
        assertThat(components.get(0)).containsExactly(1);
        int indexOfZero = components.indexOf(componentOf(components, 0));
        int indexOfTwo = components.indexOf(componentOf(components, 2));
        assertThat(indexOfZero).isGreaterThan(0);
        assertThat(indexOfTwo).isGreaterThan(0);
    }

    @Test
    void selfLoopShouldCountAsCycleMember() {
        TarjanSccFinder finder = new TarjanSccFinder(2);
        finder.addEdge(0, 0);
        assertThat(finder.hasCycle()).isTrue();
        assertThat(finder.cyclicVertices()).containsExactly(0);
        assertThat(finder.components()).hasSize(2);
    }

    @Test
    void twoConnectedCyclesShouldYieldTwoComponents() {
        TarjanSccFinder finder = new TarjanSccFinder(4);
        finder.addEdge(0, 1);
        finder.addEdge(1, 0);
        finder.addEdge(1, 2);
        finder.addEdge(2, 3);
        finder.addEdge(3, 2);
        List<List<Integer>> components = finder.components();
        assertThat(components).hasSize(2);
        assertThat(finder.cyclicVertices()).containsExactly(0, 1, 2, 3);
        // sink 侧 {2,3} 组件先出（1→2 依赖边）
        assertThat(components.get(0)).containsExactlyInAnyOrder(2, 3);
        assertThat(components.get(1)).containsExactlyInAnyOrder(0, 1);
    }

    @Test
    void deepChainShouldNotBlowStack() {
        // 2 万级链——迭代帧栈的生存证明（递归版在此深度的风险对照）
        int depth = 20_000;
        TarjanSccFinder finder = new TarjanSccFinder(depth);
        for (int i = 0; i + 1 < depth; i++) {
            finder.addEdge(i, i + 1);
        }
        List<List<Integer>> components = finder.components();
        assertThat(components).hasSize(depth);
        assertThat(finder.hasCycle()).isFalse();
    }

    @Test
    void componentsShouldBeIdempotentAndCountable() {
        TarjanSccFinder finder = new TarjanSccFinder(3);
        finder.addEdge(0, 1);
        assertThat(finder.components()).isEqualTo(finder.components());
        assertThat(finder.vertexCount()).isEqualTo(3);
        assertThat(finder.edgeCount()).isEqualTo(1);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        TarjanSccFinder finder = new TarjanSccFinder(3);
        assertThatThrownBy(() -> finder.addEdge(-1, 0)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> finder.addEdge(0, 3)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> new TarjanSccFinder(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
