package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5003 / T6108：有界负载环合同——稳定映射、容量封顶、
 * 耗尽拒配、迁移确定、畸形 fail-fast、确定性回放。
 */
class BoundedLoadRingTest {

    @Test
    void stableMappingShouldHoldWhenNotSaturated() {
        BoundedLoadRing ring = new BoundedLoadRing();
        ring.addNode("a", 100);
        ring.addNode("b", 100);
        ring.addNode("c", 100);
        Set<String> targets = new HashSet<>();
        for (int key = 0; key < 50; key++) {
            targets.add(ring.assign("key-" + key));
        }
        assertThat(targets.size()).isGreaterThanOrEqualTo(2);   // 三节点皆有机会
        assertThat(ring.assign("key-7")).isEqualTo(ring.assign("key-7"));   // 同 key 同点
    }

    @Test
    void capacityShouldCapHotNodeWithProbing() {
        BoundedLoadRing ring = new BoundedLoadRing();
        ring.addNode("a", 1);       // 容量 1——第二个命中 a 的 key 必须探查到别的节点
        ring.addNode("b", 100);
        ring.addNode("c", 100);
        int assignedToA = 0;
        for (int key = 0; key < 20; key++) {
            String node = ring.assign("hot-" + key);
            if (node.equals("a")) {
                assignedToA++;
            }
        }
        assertThat(assignedToA).isEqualTo(1);   // 上限封顶
        assertThat(ring.loadOf("a")).isEqualTo(1);
    }

    @Test
    void exhaustedCapacityShouldRefuseHonestly() {
        BoundedLoadRing ring = new BoundedLoadRing();
        ring.addNode("a", 2);
        ring.assign("k1");
        ring.assign("k2");
        assertThatThrownBy(() -> ring.assign("k3"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("耗尽");
        ring.release("a");
        assertThat(ring.assign("k3")).isEqualTo("a");   // 释放后恢复
    }

    @Test
    void removeNodeShouldKeepRemainingAssignmentsDeterministic() {
        BoundedLoadRing ring = new BoundedLoadRing();
        ring.addNode("a", 10);
        ring.addNode("b", 10);
        String firstTarget = ring.assign("sticky-key");
        ring.release(firstTarget);
        ring.removeNode(firstTarget);
        String migrated = ring.assign("sticky-key");
        assertThat(migrated).isNotEqualTo(firstTarget);   // 原节点已摘——迁移到探查序下一点
        assertThat(migrated).isEqualTo(ring.assign("sticky-key2"));   // 确定性
    }

    @Test
    void sameSequenceShouldReplaySameLoads() {
        BoundedLoadRing first = new BoundedLoadRing();
        BoundedLoadRing second = new BoundedLoadRing();
        for (BoundedLoadRing ring : new BoundedLoadRing[]{first, second}) {
            ring.addNode("n1", 5);
            ring.addNode("n2", 5);
        }
        for (int key = 0; key < 10; key++) {
            assertThat(first.assign(key)).isEqualTo(second.assign(key));
        }
        assertThat(first.loadOf("n1")).isEqualTo(second.loadOf("n1"));
    }

    @Test
    void invalidInputsShouldFailFast() {
        BoundedLoadRing ring = new BoundedLoadRing();
        assertThatThrownBy(() -> ring.addNode("a", 0)).isInstanceOf(IllegalArgumentException.class);
        ring.addNode("a", 1);
        assertThatThrownBy(() -> ring.addNode("a", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ring.release("ghost")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ring.loadOf("ghost")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ring.assign(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ring.removeNode("ghost")).isInstanceOf(IllegalArgumentException.class);
    }
}
