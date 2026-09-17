package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3032 / T5066：Maglev 合同——小素数表均匀（份额差 ≤1）、
 * 确定性、最小扰动（增一节点仅 ~1/n 键迁移）、重复即权重、
 * 全键落登记节点、参数校验。
 */
class MaglevHashTest {

    @Test
    void smallPrimeTableShouldDistributeEvenly() {
        MaglevHash maglev = new MaglevHash(List.of("a", "b", "c"), 13);
        assertThat(maglev.entriesOf("a") + maglev.entriesOf("b") + maglev.entriesOf("c"))
                .isEqualTo(13);
        assertThat(maglev.entriesOf("a")).isBetween(4L, 5L);
        assertThat(maglev.entriesOf("b")).isBetween(4L, 5L);
        assertThat(maglev.entriesOf("c")).isBetween(4L, 5L);
        assertThat(maglev.tableSize()).isEqualTo(13);
        assertThat(maglev.candidateCount()).isEqualTo(3);
    }

    @Test
    void sameKeyShouldBeDeterministic() {
        MaglevHash first = new MaglevHash(List.of("a", "b", "c"), 101);
        MaglevHash second = new MaglevHash(List.of("a", "b", "c"), 101);
        for (int i = 0; i < 500; i++) {
            String key = "key-" + i;
            assertThat(first.nodeOf(key)).isEqualTo(second.nodeOf(key));
        }
    }

    @Test
    void addingNodeShouldDisturbOnlyAFraction() {
        // 3→4 节点：期望 ~1/4 键迁移（朴素取模 ~3/4 重排的对照）
        MaglevHash before = new MaglevHash(List.of("a", "b", "c"), 101);
        MaglevHash after = new MaglevHash(List.of("a", "b", "c", "d"), 101);
        int changed = 0;
        int samples = 1_000;
        for (int i = 0; i < samples; i++) {
            String key = "disturb-" + i;
            if (!before.nodeOf(key).equals(after.nodeOf(key))) {
                changed++;
            }
        }
        assertThat(changed / (double) samples).isBetween(0.15, 0.40);
    }

    @Test
    void duplicatedCandidatesShouldActAsWeights() {
        MaglevHash maglev = new MaglevHash(List.of("big", "big", "big", "small"), 101);
        long big = maglev.entriesOf("big");
        long small = maglev.entriesOf("small");
        assertThat(big).isGreaterThan(small);
        assertThat(big / (double) small).isBetween(2.0, 4.5);   // 期望 ~3×
    }

    @Test
    void everyKeyShouldMapToARegisteredNode() {
        MaglevHash maglev = new MaglevHash(List.of("x", "y", "z", "w"), 53);
        for (int i = 0; i < 1_000; i++) {
            assertThat(maglev.nodeOf("k" + i)).isIn("x", "y", "z", "w");
        }
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new MaglevHash(List.of(), 13))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MaglevHash(List.of("a", "b"), 12))   // 非素数
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MaglevHash(List.of("a", "b", "c", "d"), 3))   // 表小于候选
                .isInstanceOf(IllegalArgumentException.class);
        MaglevHash maglev = new MaglevHash(List.of("a"), 7);
        assertThatThrownBy(() -> maglev.nodeOf(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
