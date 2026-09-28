package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7015：Manacher 合同——线性最长回文。手锚奇偶；
 * 随机串 vs 暴力圣像（长+起点最小 canonical）；fail-fast。
 */
class ManacherTest {

    @Test
    void handAnchoredCases() {
        assertThat(Manacher.longestPalindrome("babad")).isEqualTo("bab");
        assertThat(Manacher.longestPalindrome("cbbd")).isEqualTo("bb");
        assertThat(Manacher.longestPalindrome("forgeeksskeegfor")).isEqualTo("geeksskeeg");
        assertThat(Manacher.longestPalindrome("abcda")).isEqualTo("a");
        assertThat(Manacher.longestPalindrome("a")).isEqualTo("a");
        assertThat(Manacher.longestPalindrome("aa")).isEqualTo("aa");
    }

    @Test
    void randomStringsMatchBruteForce() {
        Random rng = new Random(7015L);
        for (int round = 0; round < 300; round++) {
            int n = 1 + rng.nextInt(60);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) {
                sb.append((char) ('a' + rng.nextInt(3)));
            }
            String s = sb.toString();
            String fast = Manacher.longestPalindrome(s);
            String oracle = bruteForce(s);
            assertThat(fast.length())
                    .as("串 %s", s).isEqualTo(oracle.length());
            assertThat(fast).isEqualTo(oracle);
        }
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> Manacher.longestPalindrome(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Manacher.longestPalindrome(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Manacher.longestPalindrome("ab#ba"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 暴力圣像：全部子串判回文，取（长，起点最小）最优。 */
    private String bruteForce(String s) {
        String best = "";
        for (int i = 0; i < s.length(); i++) {
            for (int j = i; j < s.length(); j++) {
                if (isPalindrome(s, i, j) && j - i + 1 > best.length()) {
                    best = s.substring(i, j + 1);
                }
            }
        }
        return best;
    }

    private boolean isPalindrome(String s, int from, int to) {
        while (from < to) {
            if (s.charAt(from) != s.charAt(to)) {
                return false;
            }
            from++;
            to--;
        }
        return true;
    }
}
