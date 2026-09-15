package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1862 / T2926：Rendezvous 哈希——确定性、最小迁移性、分布性。 */
class RendezvousHashingTest {

    /** 确定性：同键同节点集同归属。 */
    @Test
    void assignmentIsDeterministic() {
        List<String> nodes = List.of("n1", "n2", "n3");
        assertThat(RendezvousHashing.assign("key-a", nodes))
                .isEqualTo(RendezvousHashing.assign("key-a", nodes));
        assertThat(RendezvousHashing.assign("key-a", nodes)).isIn("n1", "n2", "n3");
    }

    /** 最小迁移性：摘除一节点，只有原属它的键换归属。 */
    @Test
    void nodeRemovalMovesOnlyItsOwnKeys() {
        List<String> before = List.of("n1", "n2", "n3");
        List<String> after = List.of("n1", "n2");
        List<String> keys = IntStream.range(0, 200)
                .mapToObj(i -> "key-" + i).toList();
        Map<String, String> beforeMap = RendezvousHashing.assignAll(keys, before);
        Map<String, String> afterMap = RendezvousHashing.assignAll(keys, after);
        for (String key : keys) {
            String oldOwner = beforeMap.get(key);
            String newOwner = afterMap.get(key);
            if (!oldOwner.equals(newOwner)) {
                // 迁移只允许发生在原属 n3 的键上
                assertThat(oldOwner).isEqualTo("n3");
            }
        }
    }

    /** 分布性：300 键 3 节点各有所得（HRW 统计均匀的弱断言）。 */
    @Test
    void keysSpreadAcrossNodes() {
        List<String> nodes = List.of("n1", "n2", "n3");
        Map<String, Long> counts = IntStream.range(0, 300)
                .mapToObj(i -> "spread-" + i)
                .collect(Collectors.groupingBy(k -> RendezvousHashing.assign(k, nodes),
                        Collectors.counting()));
        assertThat(counts.keySet()).containsExactlyInAnyOrder("n1", "n2", "n3");
        counts.values().forEach(c -> assertThat(c).isGreaterThan(50));
    }

    /** 畸形入参 fail-fast：空键、空节点表、空白节点、null keys。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> RendezvousHashing.assign("", List.of("n1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key 不能为空");
        assertThatThrownBy(() -> RendezvousHashing.assign("k", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nodes 不能为空");
        assertThatThrownBy(() -> RendezvousHashing.assign("k",
                java.util.Arrays.asList("n1", null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RendezvousHashing.assignAll(null, List.of("n1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
