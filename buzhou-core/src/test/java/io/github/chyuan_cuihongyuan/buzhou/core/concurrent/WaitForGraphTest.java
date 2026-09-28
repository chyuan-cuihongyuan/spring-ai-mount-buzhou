package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6045：WaitForGraph 合同——增量加边环检测死锁。
 * 无环图/二环/三环/带入口路径规范环；受难者裁决；打断后
 * 复原；fail-fast。
 */
class WaitForGraphTest {

    @Test
    void acyclicGraphNeverDeadlocks() {
        WaitForGraph g = new WaitForGraph();
        for (long id = 1; id <= 4; id++) {
            g.addNode(id);
        }
        assertThat(g.addEdge(1, 2)).isEmpty();
        assertThat(g.addEdge(2, 3)).isEmpty();
        assertThat(g.addEdge(1, 3)).isEmpty();
        assertThat(g.addEdge(4, 1)).isEmpty();
        assertThat(g.hasDeadlock()).isFalse();
        assertThat(g.deadlockVictim()).isEqualTo(-1L);
        assertThat(g.nodeCount()).isEqualTo(4);
        assertThat(g.edgeCount()).isEqualTo(4);
    }

    @Test
    void directTwoCycleDetected() {
        WaitForGraph g = new WaitForGraph();
        g.addNode(1);
        g.addNode(2);
        assertThat(g.addEdge(1, 2)).isEmpty();
        List<Long> cycle = g.addEdge(2, 1);
        assertThat(cycle).isEqualTo(List.of(1L, 2L, 1L));
        assertThat(g.hasDeadlock()).isTrue();
        assertThat(g.deadlockVictim()).isEqualTo(2L);
    }

    @Test
    void threeCycleWithEntryPointCanonical() {
        WaitForGraph g = new WaitForGraph();
        for (long id = 0; id <= 3; id++) {
            g.addNode(id);
        }
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 3);
        assertThat(g.addEdge(3, 1)).isEqualTo(List.of(1L, 2L, 3L, 1L));
        assertThat(g.deadlockVictim()).isEqualTo(3L);
    }

    @Test
    void removeVictimBreaksDeadlock() {
        WaitForGraph g = new WaitForGraph();
        for (long id = 1; id <= 3; id++) {
            g.addNode(id);
        }
        g.addEdge(1, 2);
        g.addEdge(2, 3);
        g.addEdge(3, 1);
        assertThat(g.hasDeadlock()).isTrue();
        g.removeNode(g.deadlockVictim());
        assertThat(g.hasDeadlock()).isFalse();
        assertThat(g.nodeCount()).isEqualTo(2);
        assertThat(g.edgeCount()).isEqualTo(1);
        g.addNode(3);
        g.addEdge(1, 3);
        assertThat(g.hasDeadlock()).isFalse();
    }

    @Test
    void failFastContract() {
        WaitForGraph g = new WaitForGraph();
        assertThatThrownBy(() -> g.addEdge(1, 2)).isInstanceOf(IllegalArgumentException.class);
        g.addNode(1);
        assertThatThrownBy(() -> g.addNode(1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> g.addEdge(1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> g.addEdge(1, 99)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> g.removeNode(99)).isInstanceOf(IllegalArgumentException.class);
    }
}
