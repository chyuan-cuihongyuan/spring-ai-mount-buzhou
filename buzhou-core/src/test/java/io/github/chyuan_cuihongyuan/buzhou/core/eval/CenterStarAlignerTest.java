package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CenterStarAligner 契约测试（spec 10022 / X10046）：同序恒等 +
 * 投影还原 + 行等长 + 比对收益 + 确定性 + fail-fast。
 */
class CenterStarAlignerTest {

    @Test
    void shouldKeepIdenticalSequencesInOneColumn() {
        List<String> aligned = CenterStarAligner.align(List.of("ACGT", "ACGT", "ACGT"));
        assertThat(aligned).containsExactly("ACGT", "ACGT", "ACGT");
    }

    @Test
    void shouldProjectBackToOriginalSequences() {
        List<String> sequences = List.of("GATTACA", "GCATGCG", "GATTACG");
        List<String> aligned = CenterStarAligner.align(sequences);
        assertThat(aligned).hasSameSizeAs(sequences);
        int rowLength = aligned.get(0).length();
        for (int r = 0; r < aligned.size(); r++) {
            assertThat(aligned.get(r).replace("-", "")).as("行 %d 投影还原", r)
                    .isEqualTo(sequences.get(r));
            assertThat(aligned.get(r).length()).as("行 %d 等长", r).isEqualTo(rowLength);
        }
    }

    @Test
    void shouldImprovePairwiseSumOverUnaligned() {
        List<String> sequences = List.of("GATTACA", "GCATGCG");
        List<String> aligned = CenterStarAligner.align(sequences);
        int alignedScore = score(aligned.get(0), aligned.get(1));
        assertThat(alignedScore).isGreaterThanOrEqualTo(score("GATTACA", "GCATGCG"));
    }

    @Test
    void shouldBeDeterministic() {
        List<String> sequences = List.of("GATTACA", "GCATGCG", "GATTACG", "GATTAGA");
        List<String> first = CenterStarAligner.align(sequences);
        List<String> second = CenterStarAligner.align(sequences);
        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldFailFastOnContractViolations() {
        assertThatThrownBy(() -> CenterStarAligner.align(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CenterStarAligner.align(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CenterStarAligner.align(List.of("AC", "")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private int score(String first, String second) {
        int total = 0;
        for (int i = 0; i < first.length(); i++) {
            char a = first.charAt(i);
            char b = second.charAt(i);
            if (a == '-' || b == '-') {
                total += -2;
            } else if (a == b) {
                total += 1;
            } else {
                total += -1;
            }
        }
        return total;
    }
}
