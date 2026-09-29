package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BitapSearchTest {

    @Test
    void shouldMatchHandAnchors() {
        assertThat(BitapSearch.findAll("aabcfaab", "aab")).containsExactly(0, 5);
        assertThat(BitapSearch.findAll("aaaa", "aa")).containsExactly(0, 1, 2);
        assertThat(BitapSearch.findAll("abababab", "abab")).containsExactly(0, 2, 4);
        assertThat(BitapSearch.findAll("abc", "abc")).containsExactly(0);
        assertThat(BitapSearch.findAll("abc", "xyz")).isEmpty();
        assertThat(BitapSearch.first("aabcfaab", "faa")).isEqualTo(4);
    }

    @Test
    void shouldAcceptMaxWidthAndRejectOverflow() {
        String maxPattern = "x".repeat(63);
        assertThat(BitapSearch.findAll("y" + maxPattern, maxPattern)).containsExactly(1);
        assertThat(BitapSearch.findAll(maxPattern, maxPattern)).containsExactly(0);
        String overPattern = "x".repeat(64);
        assertThatThrownBy(() -> BitapSearch.findAll("x", overPattern))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldMatchIndexOfOracleOnRandomInputs() {
        Random random = new Random(8002);
        for (int round = 0; round < 300; round++) {
            String text = randomString(random, 1 + random.nextInt(40));
            String pattern = randomString(random, 1 + random.nextInt(6));
            List<Integer> expected = new java.util.ArrayList<>();
            int at = text.indexOf(pattern);
            while (at >= 0) {
                expected.add(at);
                at = text.indexOf(pattern, at + 1);
            }
            assertThat(BitapSearch.findAll(text, pattern))
                    .as("text=%s pattern=%s", text, pattern)
                    .containsExactlyElementsOf(expected);
        }
    }

    @Test
    void shouldFailFastOnNullOrEmpty() {
        assertThatThrownBy(() -> BitapSearch.findAll(null, "a"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BitapSearch.findAll("a", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BitapSearch.findAll("a", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static String randomString(Random random, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + random.nextInt(3)));
        }
        return sb.toString();
    }
}
