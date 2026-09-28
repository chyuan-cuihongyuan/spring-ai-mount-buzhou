package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7031：EulerianPath 合同——Hierholzer 一笔画。回路/
 * 路径手锚（逐边合法+边数+1）；度数违反/不连通 fail-fast。
 */
class EulerianPathTest {

    @Test
    void eulerianCycleHandAnchor() {
        Set<Long> edges = Set.of(key(0, 1), key(1, 2), key(2, 0));
        EulerianPath g = new EulerianPath(3);
        edges.forEach(e -> g.addEdge((int) (e / 10), (int) (e % 10)));
        List<Integer> path = g.eulerianPath();
        assertThat(path).hasSize(4);
        assertThat(path.get(0)).isEqualTo(path.get(3));
        for (int i = 0; i + 1 < path.size(); i++) {
            assertThat(edges).contains(key(path.get(i), path.get(i + 1)));
        }
    }

    @Test
    void eulerianTrailHandAnchor() {
        EulerianPath g = new EulerianPath(4);
        Set<Long> edges = new HashSet<>();
        int[][] pairs = {{0, 1}, {1, 2}, {2, 3}, {3, 1}};
        for (int[] p : pairs) {
            g.addEdge(p[0], p[1]);
            edges.add(key(p[0], p[1]));
        }
        List<Integer> path = g.eulerianPath();
        assertThat(path).hasSize(5);
        assertThat(path.get(0)).isEqualTo(0);
        assertThat(path.get(path.size() - 1)).isEqualTo(1);
        for (int i = 0; i + 1 < path.size(); i++) {
            assertThat(edges).contains(key(path.get(i), path.get(i + 1)));
        }
    }

    @Test
    void degreeViolationFailsFast() {
        EulerianPath g = new EulerianPath(3);
        g.addEdge(0, 1);
        g.addEdge(2, 1);
        assertThatThrownBy(g::eulerianPath).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void disconnectedFailsFast() {
        EulerianPath g = new EulerianPath(5);
        g.addEdge(0, 1);
        g.addEdge(1, 0);
        g.addEdge(2, 3);
        g.addEdge(3, 2);
        assertThatThrownBy(g::eulerianPath).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new EulerianPath(0)).isInstanceOf(IllegalArgumentException.class);
        EulerianPath g = new EulerianPath(2);
        assertThatThrownBy(() -> g.addEdge(0, 2)).isInstanceOf(IllegalArgumentException.class);
    }

    private long key(int from, int to) {
        return (long) from * 10 + to;
    }
}
