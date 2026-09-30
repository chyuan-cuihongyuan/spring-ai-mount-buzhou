package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HopcroftKarpMatcherTest {

    @Test
    void shouldMatchClassicBipartiteAnchor() {
        // 经典锚：4×4，最大匹配 3（L0→R0, L1→R2, L2→R1；L3 只连已被更强占的 R0/R1）
        int[][] edges = {{0, 0}, {0, 1}, {1, 2}, {2, 1}, {3, 0}, {3, 1}};
        assertThat(HopcroftKarpMatcher.maxMatchingSize(4, 3, edges)).isEqualTo(3);
        int[] matchLeft = HopcroftKarpMatcher.matching(4, 3, edges);
        // 大小 3 承诺；分布不唯一（L0/L3 谁落单皆合法）——校验匹配合法性：非 -1 项互异且都在边集内
        java.util.Set<Integer> usedRights = new java.util.HashSet<>();
        int matched = 0;
        for (int l = 0; l < 4; l++) {
            if (matchLeft[l] != -1) {
                matched++;
                usedRights.add(matchLeft[l]);
            }
        }
        assertThat(matched).isEqualTo(3);
        assertThat(usedRights).hasSize(3);
        assertThat(matchLeft[1]).isEqualTo(2);
    }

    @Test
    void shouldHandleDegenerateShapes() {
        assertThat(HopcroftKarpMatcher.matching(0, 0, new int[][]{})).isEmpty();
        int[] none = HopcroftKarpMatcher.matching(2, 2, new int[][]{});
        assertThat(none).containsExactly(-1, -1);
        // 完美匹配（链式竞争：L0 抢 R0 后 L1 顶替、L0 顺延 R1）
        int[] chain = HopcroftKarpMatcher.matching(2, 2, new int[][]{{0, 0}, {1, 0}, {0, 1}});
        assertThat(chain[0]).isNotEqualTo(chain[1]);
        assertThat(chain[0]).isNotEqualTo(-1);
        assertThat(chain[1]).isNotEqualTo(-1);
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        int[][] edges = {{0, 0}, {1, 1}, {0, 1}};
        int[] first = HopcroftKarpMatcher.matching(2, 2, edges);
        int[] second = HopcroftKarpMatcher.matching(2, 2, edges);
        assertThat(first).isEqualTo(second);
        assertThatThrownBy(() -> HopcroftKarpMatcher.matching(-1, 2, edges))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HopcroftKarpMatcher.matching(2, 2, new int[][]{{2, 0}}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HopcroftKarpMatcher.matching(2, 2, new int[][]{{0, 2}}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldBeatGreedyOnTraps() {
        // 贪心陷阱：先到先得可被坏取序钉在 1，最大匹配必为 2
        int[][] trap = {{0, 0}, {0, 1}, {1, 0}};
        assertThat(HopcroftKarpMatcher.maxMatchingSize(2, 2, trap)).isEqualTo(2);
        // 随机图圣像：HK 大小 ≤ min(L,R) 且 ≤ 边数；确定性双跑全等
        Random random = new Random(42);
        for (int t = 0; t < 50; t++) {
            int left = 1 + random.nextInt(8);
            int right = 1 + random.nextInt(8);
            int[][] es = new int[random.nextInt(20)][];
            for (int i = 0; i < es.length; i++) {
                es[i] = new int[]{random.nextInt(left), random.nextInt(right)};
            }
            int size = HopcroftKarpMatcher.maxMatchingSize(left, right, es);
            assertThat(size).isLessThanOrEqualTo(Math.min(left, right));
            assertThat(size).isLessThanOrEqualTo(es.length);
            assertThat(HopcroftKarpMatcher.matching(left, right, es))
                    .isEqualTo(HopcroftKarpMatcher.matching(left, right, es));
        }
    }
}
