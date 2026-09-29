package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MinWindowSubstringTest {

    @Test
    void shouldMatchHandAnchors() {
        assertThat(MinWindowSubstring.minWindow("ADOBECODEBANC", "ABC")).isEqualTo("BANC");
        assertThat(MinWindowSubstring.minWindow("a", "a")).isEqualTo("a");
        assertThat(MinWindowSubstring.minWindow("a", "aa")).isEmpty();
        assertThat(MinWindowSubstring.minWindow("aa", "aa")).isEqualTo("aa");
        assertThat(MinWindowSubstring.minWindow("ab", "b")).isEqualTo("b");
        assertThat(MinWindowSubstring.minWindow("bba", "ab")).isEqualTo("ba");
        assertThat(MinWindowSubstring.minWindow("aabbaabbcc", "abc")).isEqualTo("abbc");
    }

    @Test
    void shouldMatchBruteForceOracleOnRandomInputs() {
        Random random = new Random(8014);
        for (int round = 0; round < 300; round++) {
            String text = randomString(random, 1 + random.nextInt(24));
            String pattern = randomString(random, 1 + random.nextInt(3));
            String expected = bruteMinWindow(text, pattern);
            assertThat(MinWindowSubstring.minWindow(text, pattern))
                    .as("text=%s pattern=%s", text, pattern)
                    .isEqualTo(expected);
        }
    }

    @Test
    void shouldFailFastOnNullAndEmptyPattern() {
        assertThatThrownBy(() -> MinWindowSubstring.minWindow(null, "a"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MinWindowSubstring.minWindow("a", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MinWindowSubstring.minWindow("a", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 圣像：全起点扩张枚举取（长度, 起点）字典序最小。 */
    private static String bruteMinWindow(String text, String pattern) {
        String best = null;
        for (int left = 0; left < text.length(); left++) {
            int[] need = new int[128];
            for (int i = 0; i < pattern.length(); i++) {
                need[pattern.charAt(i)]++;
            }
            int missing = pattern.length();
            for (int right = left; right < text.length(); right++) {
                if (need[text.charAt(right)] > 0) {
                    missing--;
                }
                need[text.charAt(right)]--;
                if (missing == 0) {
                    String candidate = text.substring(left, right + 1);
                    if (best == null || candidate.length() < best.length()) {
                        best = candidate;
                    }
                    break;
                }
            }
        }
        return best == null ? "" : best;
    }

    private static String randomString(Random random, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + random.nextInt(4)));
        }
        return sb.toString();
    }
}
