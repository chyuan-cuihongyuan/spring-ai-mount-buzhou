package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GlobMatcherTest {

    @Test
    void shouldMatchHandAnchors() {
        assertThat(GlobMatcher.matches("*.java", "Foo.java")).isTrue();
        assertThat(GlobMatcher.matches("*.java", "foo.txt")).isFalse();
        assertThat(GlobMatcher.matches("a?c", "abc")).isTrue();
        assertThat(GlobMatcher.matches("a?c", "ac")).isFalse();
        assertThat(GlobMatcher.matches("[a-c]x", "ax")).isTrue();
        assertThat(GlobMatcher.matches("[a-c]x", "bx")).isTrue();
        assertThat(GlobMatcher.matches("[a-c]x", "dx")).isFalse();
        assertThat(GlobMatcher.matches("[!a]x", "bx")).isTrue();
        assertThat(GlobMatcher.matches("[!a]x", "ax")).isFalse();
        assertThat(GlobMatcher.matches("[]a]x", "]x")).isTrue();
        assertThat(GlobMatcher.matches("a*b*c", "aXbYc")).isTrue();
        assertThat(GlobMatcher.matches("a*b*c", "acb")).isFalse();
        assertThat(GlobMatcher.matches("*", "")).isTrue();
        assertThat(GlobMatcher.matches("**", "")).isTrue();
        assertThat(GlobMatcher.matches("?", "")).isFalse();
        assertThat(GlobMatcher.matches("", "")).isTrue();
        assertThat(GlobMatcher.matches("", "a")).isFalse();
    }

    @Test
    void shouldMatchRecursiveOracleOnRandomInputs() {
        Random random = new Random(8004);
        String patternAlphabet = "ab*?";
        for (int round = 0; round < 300; round++) {
            String pattern = randomString(random, patternAlphabet, random.nextInt(7));
            String text = randomString(random, "ab", random.nextInt(9));
            assertThat(GlobMatcher.matches(pattern, text))
                    .as("pattern=%s text=%s", pattern, text)
                    .isEqualTo(brute(pattern, 0, text, 0));
        }
    }

    @Test
    void shouldFailFastOnNullAndUnclosedClass() {
        assertThatThrownBy(() -> GlobMatcher.matches(null, "a"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GlobMatcher.matches("a", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GlobMatcher.matches("a[b", "ab"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GlobMatcher.matches("[z-a]", "m"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 圣像：朴素递归（* 与 ? 域）。 */
    private static boolean brute(String p, int pi, String s, int si) {
        if (pi == p.length()) {
            return si == s.length();
        }
        char pc = p.charAt(pi);
        if (pc == '*') {
            for (int k = si; k <= s.length(); k++) {
                if (brute(p, pi + 1, s, k)) {
                    return true;
                }
            }
            return false;
        }
        if (si < s.length() && (pc == '?' || pc == s.charAt(si))) {
            return brute(p, pi + 1, s, si + 1);
        }
        return false;
    }

    private static String randomString(Random random, String alphabet, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
