package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NussinovFolder 契约测试（spec 10020 / X10042）：手锚发夹 + 无配
 * 序列 + 无假结性质 + 最小环长 + 回溯自洽 + fail-fast。
 */
class NussinovFolderTest {

    @Test
    void shouldFoldHairpinIntoThreePairs() {
        NussinovFolder.Structure structure = NussinovFolder.fold("GGGAAACCC");
        assertThat(structure.maxPairs()).isEqualTo(3);
        assertThat(structure.pairs()).hasSize(3);
    }

    @Test
    void shouldReturnZeroPairsForUnpairableSequence() {
        NussinovFolder.Structure structure = NussinovFolder.fold("AAAAAAAAA");
        assertThat(structure.maxPairs()).isZero();
        assertThat(structure.pairs()).isEmpty();
    }

    @Test
    void shouldProduceNonCrossingPairs() {
        NussinovFolder.Structure structure = NussinovFolder.fold("GGACCAGACC GG".replace(" ", "") + "AAACCC");
        for (int a = 0; a < structure.pairs().size(); a++) {
            for (int b = a + 1; b < structure.pairs().size(); b++) {
                int[] first = structure.pairs().get(a);
                int[] second = structure.pairs().get(b);
                boolean crossing = first[0] < second[0] && second[0] < first[1]
                        && first[1] < second[1];
                assertThat(crossing).as("配对无假结 %s vs %s", first, second).isFalse();
            }
        }
    }

    @Test
    void shouldRespectMinimumLoopLength() {
        NussinovFolder.Structure structure = NussinovFolder.fold("GGGGCCCCCAAAAUGCGCAAA");
        for (int[] pair : structure.pairs()) {
            assertThat(pair[1] - pair[0] - 1).as("配对 %s 环长 ≥3", pair).isGreaterThanOrEqualTo(3);
        }
    }

    @Test
    void shouldSelfConsistBetweenCountAndPairs() {
        String[] sequences = {"GGGAAACCC", "GGCCAAAGGCC", "ACGUACGUACGU", "GGGGCCCCCAAAAUGCGCAAA"};
        for (String seq : sequences) {
            NussinovFolder.Structure structure = NussinovFolder.fold(seq);
            assertThat(structure.pairs()).as("序列 %s 计数与配对集自洽", seq)
                    .hasSize(structure.maxPairs());
        }
    }

    @Test
    void shouldFailFastOnContractViolations() {
        assertThatThrownBy(() -> NussinovFolder.fold(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NussinovFolder.fold("GC"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NussinovFolder.fold("GGGAAACCCX"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NussinovFolder.fold("GGGAATCCC"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
