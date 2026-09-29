package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoyerMooreSearchTest {

    @Test
    void shouldMatchHandAnchors() {
        assertThat(BoyerMooreSearch.findAll("aabcfaab", "aab")).containsExactly(0, 5);
        assertThat(BoyerMooreSearch.findAll("aaaa", "aa")).containsExactly(0, 1, 2);
        assertThat(BoyerMooreSearch.findAll("abababab", "abab")).containsExactly(0, 2, 4);
        assertThat(BoyerMooreSearch.findAll("abcabc", "c")).containsExactly(2, 5);
        assertThat(BoyerMooreSearch.findAll("abc", "abc")).containsExactly(0);
        assertThat(BoyerMooreSearch.findAll("abc", "xyz")).isEmpty();
        assertThat(BoyerMooreSearch.findAll("ab", "abc")).isEmpty();
        assertThat(BoyerMooreSearch.first("aabcfaab", "faa")).isEqualTo(4);
        assertThat(BoyerMooreSearch.first("abc", "z")).isEqualTo(-1);
    }

    @Test
    void shouldMatchIndexOfOracleOnRandomInputs() {
        Random random = new Random(8001);
        for (int round = 0; round < 300; round++) {
            String text = randomString(random, 1 + random.nextInt(40));
            String pattern = randomString(random, 1 + random.nextInt(5));
            List<Integer> expected = new java.util.ArrayList<>();
            int at = text.indexOf(pattern);
            while (at >= 0) {
                expected.add(at);
                at = text.indexOf(pattern, at + 1);
            }
            assertThat(BoyerMooreSearch.findAll(text, pattern))
                    .as("text=%s pattern=%s", text, pattern)
                    .containsExactlyElementsOf(expected);
        }
    }

    @Test
    void shouldFailFastOnNullOrEmpty() {
        assertThatThrownBy(() -> BoyerMooreSearch.findAll(null, "a"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BoyerMooreSearch.findAll("a", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BoyerMooreSearch.findAll("a", ""))
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
