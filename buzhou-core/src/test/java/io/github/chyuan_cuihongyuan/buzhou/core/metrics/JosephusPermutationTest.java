package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7040：JosephusPermutation 合同——计数出列序。经典
 * 手锚（n=5,k=2；n=7,k=3）；k=1 顺序出列；幸存者一致；
 * fail-fast。
 */
class JosephusPermutationTest {

    @Test
    void classicHandAnchors() {
        assertThat(JosephusPermutation.permutation(5, 2))
                .containsExactly(1, 3, 0, 4, 2);
        assertThat(JosephusPermutation.permutation(7, 3))
                .containsExactly(2, 5, 1, 6, 4, 0, 3);
        assertThat(JosephusPermutation.survivor(5, 2)).isEqualTo(2);
        assertThat(JosephusPermutation.survivor(7, 3)).isEqualTo(3);
    }

    @Test
    void stepOneIsSequential() {
        assertThat(JosephusPermutation.permutation(6, 1))
                .containsExactly(0, 1, 2, 3, 4, 5);
        assertThat(JosephusPermutation.survivor(6, 1)).isEqualTo(5);
    }

    @Test
    void permutationIsFullCoverage() {
        List<Integer> perm = JosephusPermutation.permutation(9, 4);
        assertThat(perm).hasSize(9);
        assertThat(perm.stream().distinct().count()).isEqualTo(9);
        assertThat(JosephusPermutation.survivor(9, 4))
                .isEqualTo(perm.get(8));
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> JosephusPermutation.permutation(0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JosephusPermutation.permutation(5, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JosephusPermutation.survivor(-1, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
