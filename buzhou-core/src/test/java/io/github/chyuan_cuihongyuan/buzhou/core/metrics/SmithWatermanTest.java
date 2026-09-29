package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmithWatermanTest {

    private static final int MATCH = 3;
    private static final int MISMATCH = -2;
    private static final int GAP = -2;

    @Test
    void shouldFindEmbeddedLocalMatch() {
        SmithWaterman.Alignment alignment = SmithWaterman.align(
                "TTTTACGTAGGGG", "CCACGTACAAA", MATCH, MISMATCH, GAP);
        assertThat(alignment.bestScore()).isGreaterThanOrEqualTo(12);
        assertThat(alignment.first()).contains("ACGT");
        assertThat(SmithWaterman.align("AAAA", "AAAA", MATCH, MISMATCH, GAP).bestScore()).isEqualTo(12);
        assertThat(SmithWaterman.align("AAAA", "TTTT", MATCH, MISMATCH, GAP).bestScore()).isEqualTo(0);
        assertThat(SmithWaterman.align("", "", MATCH, MISMATCH, GAP).bestScore()).isEqualTo(0);
    }

    @Test
    void shouldMatchSubstringPairOracleOnRandomInputs() {
        Random random = new Random(8044);
        for (int round = 0; round < 300; round++) {
            String first = randomString(random, 1 + random.nextInt(7));
            String second = randomString(random, 1 + random.nextInt(7));
            int expected = bruteMaxLocal(first, second);
            assertThat(SmithWaterman.align(first, second, MATCH, MISMATCH, GAP).bestScore())
                    .as("first=%s second=%s", first, second)
                    .isEqualTo(expected);
        }
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        SmithWaterman.Alignment first = SmithWaterman.align("TTTTACGTAGGGG", "CCACGTACAAA", 3, -2, -2);
        SmithWaterman.Alignment second = SmithWaterman.align("TTTTACGTAGGGG", "CCACGTACAAA", 3, -2, -2);
        assertThat(first.first()).isEqualTo(second.first());
        assertThatThrownBy(() -> SmithWaterman.align(null, "a", 1, -1, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SmithWaterman.align("a", null, 1, -1, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 圣像：全子串对 NW 全局得分取 max（局部最优=最优子串对全局）。 */
    private static int bruteMaxLocal(String first, String second) {
        int best = 0;
        for (int i = 0; i < first.length(); i++) {
            for (int a = i + 1; a <= first.length(); a++) {
                for (int j = 0; j < second.length(); j++) {
                    for (int b = j + 1; b <= second.length(); b++) {
                        int score = NeedlemanWunsch.align(first.substring(i, a),
                                second.substring(j, b), MATCH, MISMATCH, GAP).score();
                        best = Math.max(best, score);
                    }
                }
            }
        }
        return best;
    }

    private static String randomString(Random random, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('A' + random.nextInt(4)));
        }
        return sb.toString();
    }
}
