package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LevenshteinAutomatonTest {

    @Test
    void shouldMatchHandAnchors() {
        LevenshteinAutomaton kitten = LevenshteinAutomaton.of("kitten", 2);
        assertThat(kitten.matches("kitchen")).isTrue();
        assertThat(kitten.matches("sitting")).isFalse();
        assertThat(kitten.matches("kitten")).isTrue();
        assertThat(kitten.distanceTo("kitchen")).isEqualTo(2);
        assertThat(LevenshteinAutomaton.of("abc", 3).matches("")).isTrue();
        assertThat(LevenshteinAutomaton.of("abc", 2).matches("")).isFalse();
    }

    @Test
    void shouldMatchFullMatrixOracleOnRandomInputs() {
        Random random = new Random(8003);
        for (int round = 0; round < 300; round++) {
            String pattern = randomString(random, 1 + random.nextInt(6));
            String word = randomString(random, random.nextInt(9));
            int maxEdits = random.nextInt(4);
            int expected = bruteDistance(pattern, word);
            boolean expectedAccept = expected <= maxEdits;
            assertThat(LevenshteinAutomaton.of(pattern, maxEdits).matches(word))
                    .as("pattern=%s word=%s k=%d brute=%d", pattern, word, maxEdits, expected)
                    .isEqualTo(expectedAccept);
        }
    }

    @Test
    void shouldFailFastOnBadConstruction() {
        assertThatThrownBy(() -> LevenshteinAutomaton.of(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LevenshteinAutomaton.of("", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LevenshteinAutomaton.of("a", -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LevenshteinAutomaton.of("a", 1).matches(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 圣像：全矩阵 DP 编辑距离（两行滚动）。 */
    private static int bruteDistance(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            d[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
            }
        }
        return d[a.length()][b.length()];
    }

    private static String randomString(Random random, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + random.nextInt(3)));
        }
        return sb.toString();
    }
}
