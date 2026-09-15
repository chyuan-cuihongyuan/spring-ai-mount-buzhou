package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1828 / T2858：平滑加权——比例保持、交错平滑、确定性。 */
class SmoothWeightedSequenceTest {

    /** 比例保持：5:1:2 在 80 次派发中恰好 50/10/20。 */
    @Test
    void proportionsHoldExactly() {
        long[] counts = SmoothWeightedSequence.counts(List.of(5, 1, 2), 80);
        assertThat(counts[0]).isEqualTo(50L);
        assertThat(counts[1]).isEqualTo(10L);
        assertThat(counts[2]).isEqualTo(20L);
    }

    /** 平滑性：无三连同派（NGINX smooth 的核心承诺——不扎堆）。 */
    @Test
    void noTripleConsecutiveSamePick() {
        List<Integer> seq = SmoothWeightedSequence.sequence(List.of(5, 1, 2), 60);
        for (int i = 2; i < seq.size(); i++) {
            boolean notTriple = !seq.get(i).equals(seq.get(i - 1))
                    || !seq.get(i).equals(seq.get(i - 2));
            assertThat(notTriple).as("位置 %d 不应三连", i).isTrue();
        }
        // 首个高频项后紧跟低频项（穿插而非前缀扎堆）
        assertThat(seq.subList(0, 6)).contains(1);
    }

    /** 零权重不参与；确定性（同参同序）。 */
    @Test
    void zeroWeightExcludedAndDeterministic() {
        List<Integer> first = SmoothWeightedSequence.sequence(List.of(0, 3), 6);
        List<Integer> second = SmoothWeightedSequence.sequence(List.of(0, 3), 6);
        assertThat(first).containsOnly(1);
        assertThat(first).isEqualTo(second);
    }

    /** 零取数为空序列。 */
    @Test
    void zeroPicksYieldEmpty() {
        assertThat(SmoothWeightedSequence.sequence(List.of(1, 1), 0)).isEmpty();
    }

    /** 畸形入参 fail-fast：负 picks、空/含负权重、全零权重。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> SmoothWeightedSequence.sequence(List.of(1), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("picks 不能为负");
        assertThatThrownBy(() -> SmoothWeightedSequence.sequence(List.of(), 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weights 不能为空");
        assertThatThrownBy(() -> SmoothWeightedSequence.sequence(List.of(1, -1), 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SmoothWeightedSequence.sequence(List.of(0, 0), 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("权重总和须大于 0");
    }
}
