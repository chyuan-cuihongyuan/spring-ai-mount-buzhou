package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NeedlemanWunschTest {

    private static final int MATCH = 1;
    private static final int MISMATCH = -1;
    private static final int GAP = -1;

    @Test
    void shouldMatchHandAnchors() {
        NeedlemanWunsch.Alignment alignment = NeedlemanWunsch.align("GATTACA", "GCATGCU", MATCH, MISMATCH, GAP);
        assertThat(alignment.score()).isEqualTo(0);
        assertThat(alignment.first().length()).isEqualTo(alignment.second().length());
        assertThat(NeedlemanWunsch.align("ACGT", "ACGT", MATCH, MISMATCH, GAP).score()).isEqualTo(4);
        assertThat(NeedlemanWunsch.align("", "", MATCH, MISMATCH, GAP).score()).isEqualTo(0);
        assertThat(NeedlemanWunsch.align("A", "", MATCH, MISMATCH, GAP).score()).isEqualTo(-1);
    }

    @Test
    void shouldMatchMemoizedRecursionOracleOnRandomInputs() {
        Random random = new Random(8043);
        for (int round = 0; round < 300; round++) {
            String first = randomString(random, 1 + random.nextInt(7));
            String second = randomString(random, 1 + random.nextInt(7));
            Map<String, Integer> memo = new HashMap<>();
            int expected = brute(first.length(), second.length(), first, second, memo);
            assertThat(NeedlemanWunsch.align(first, second, MATCH, MISMATCH, GAP).score())
                    .as("first=%s second=%s", first, second)
                    .isEqualTo(expected);
        }
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        NeedlemanWunsch.Alignment first = NeedlemanWunsch.align("GATTACA", "GCATGCU", MATCH, MISMATCH, GAP);
        NeedlemanWunsch.Alignment second = NeedlemanWunsch.align("GATTACA", "GCATGCU", MATCH, MISMATCH, GAP);
        assertThat(first.first()).isEqualTo(second.first());
        assertThat(first.second()).isEqualTo(second.second());
        assertThatThrownBy(() -> NeedlemanWunsch.align(null, "a", MATCH, MISMATCH, GAP))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NeedlemanWunsch.align("a", null, MATCH, MISMATCH, GAP))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 圣像：记忆化递归（同递推式）。 */
    private static int brute(int i, int j, String first, String second, Map<String, Integer> memo) {
        if (i == 0) {
            return j * GAP;
        }
        if (j == 0) {
            return i * GAP;
        }
        String key = i + ":" + j;
        Integer cached = memo.get(key);
        if (cached != null) {
            return cached;
        }
        int diagonal = brute(i - 1, j - 1, first, second, memo)
                + (first.charAt(i - 1) == second.charAt(j - 1) ? MATCH : MISMATCH);
        int up = brute(i - 1, j, first, second, memo) + GAP;
        int left = brute(i, j - 1, first, second, memo) + GAP;
        int result = Math.max(diagonal, Math.max(up, left));
        memo.put(key, result);
        return result;
    }

    private static String randomString(Random random, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('A' + random.nextInt(4)));
        }
        return sb.toString();
    }
}
