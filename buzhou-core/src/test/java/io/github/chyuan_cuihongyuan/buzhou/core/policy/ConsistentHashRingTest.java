package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2025 / T3152：一致性哈希环合同——确定性归属、分布均衡、删节点
 * 最小迁移（仅原归属者变）、加节点吸收近段、回绕、畸形 fail-fast。
 */
class ConsistentHashRingTest {

    private static Map<String, Integer> distribution(ConsistentHashRing ring, int keys) {
        Map<String, Integer> dist = new HashMap<>();
        for (int i = 0; i < keys; i++) {
            dist.merge(ring.nodeFor("key-" + i), 1, Integer::sum);
        }
        return dist;
    }

    @Test
    void sameKeyShouldAlwaysMapToSameNode() {
        ConsistentHashRing ring = new ConsistentHashRing();
        ring.addNode("a");
        ring.addNode("b");
        assertThat(ring.nodeFor("stable-key")).isEqualTo(ring.nodeFor("stable-key"));
        assertThat(ring.nodeFor("stable-key")).isIn("a", "b");
    }

    @Test
    void distributionShouldBeReasonablyEven() {
        ConsistentHashRing ring = new ConsistentHashRing();
        ring.addNode("n1");
        ring.addNode("n2");
        ring.addNode("n3");
        Map<String, Integer> dist = distribution(ring, 30_000);
        assertThat(dist).containsKeys("n1", "n2", "n3"); // 三节点都有份额
        // 均衡界：每节点 ∈ [20%, 47%]（虚节点 160 下典型离散范围）
        dist.forEach((node, count) -> {
            double share = count / 30_000.0;
            assertThat(share).as("节点 %s 份额 %s", node, share).isBetween(0.20d, 0.47d);
        });
    }

    @Test
    void removingNodeShouldOnlyMigrateItsOwnKeys() {
        ConsistentHashRing ring = new ConsistentHashRing();
        ring.addNode("n1");
        ring.addNode("n2");
        ring.addNode("n3");
        Map<String, String> before = new HashMap<>();
        for (int i = 0; i < 10_000; i++) {
            before.put("key-" + i, ring.nodeFor("key-" + i));
        }
        Map<String, Integer> distBefore = distribution(ring, 10_000);
        ring.removeNode("n2");
        int migrated = 0;
        for (int i = 0; i < 10_000; i++) {
            String after = ring.nodeFor("key-" + i);
            if (!after.equals(before.get("key-" + i))) {
                migrated++;
                // 最小迁移语义：变键的原归属必是被删节点
                assertThat(before.get("key-" + i)).isEqualTo("n2");
                assertThat(after).isIn("n1", "n3"); // 迁移目标只在幸存者
            }
        }
        // 迁移量 = n2 原份额（非全量）
        assertThat(migrated).isEqualTo(distBefore.get("n2"));
        assertThat(migrated).isLessThan(5_000); // 远小于全量 10000
    }

    @Test
    void addingNodeShouldAbsorbOnlyNearSegment() {
        ConsistentHashRing ring = new ConsistentHashRing();
        ring.addNode("n1");
        ring.addNode("n2");
        Map<String, String> before = new HashMap<>();
        for (int i = 0; i < 10_000; i++) {
            before.put("key-" + i, ring.nodeFor("key-" + i));
        }
        ring.addNode("n3");
        int migrated = 0;
        for (int i = 0; i < 10_000; i++) {
            if (!ring.nodeFor("key-" + i).equals(before.get("key-" + i))) {
                migrated++;
                assertThat(ring.nodeFor("key-" + i)).isEqualTo("n3"); // 新键必归新节点
            }
        }
        assertThat(migrated).isGreaterThan(0);  // 吸收了近段
        assertThat(migrated).isLessThan(5_000); // 远小于全量
    }

    @Test
    void emptyRingShouldReturnNull() {
        assertThat(new ConsistentHashRing().nodeFor("k")).isNull();
    }

    @Test
    void ringShouldWrapAround() {
        ConsistentHashRing ring = new ConsistentHashRing(1); // 单虚节点
        ring.addNode("solo");
        // 任意键都归唯一节点（回绕路径覆盖）
        for (int i = 0; i < 100; i++) {
            assertThat(ring.nodeFor("k-" + i)).isEqualTo("solo");
        }
        assertThat(ring.virtualNodeCount()).isEqualTo(1);
    }

    @Test
    void malformedInputsShouldFailFast() {
        ConsistentHashRing ring = new ConsistentHashRing();
        assertThatThrownBy(() -> new ConsistentHashRing(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ring.addNode(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ring.addNode(" "))
                .isInstanceOf(IllegalArgumentException.class);
        ring.addNode("a");
        assertThatThrownBy(() -> ring.addNode("a")) // 重复
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已存在");
        assertThatThrownBy(() -> ring.removeNode("ghost"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ring.nodeFor(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
