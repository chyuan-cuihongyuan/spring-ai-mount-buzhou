package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7001：ZArray 合同——Z 函数线性前缀面。手锚小例；
 * 随机串 vs 暴力 LCP 圣像逐步全等；findAll vs indexOf 圣像；
 * 重叠命中；fail-fast。
 */
class ZArrayTest {

    @Test
    void handAnchoredSmallCases() {
        assertThat(ZArray.zFunction("aaaa")).containsExactly(4, 3, 2, 1);
        assertThat(ZArray.zFunction("abacaba")).containsExactly(7, 0, 1, 0, 3, 0, 1);
        assertThat(ZArray.zFunction("aabxaab")).containsExactly(7, 1, 0, 0, 3, 1, 0);
    }

    @Test
    void randomStringsMatchBruteForceLcp() {
        Random rng = new Random(7001L);
        for (int round = 0; round < 200; round++) {
            int n = 1 + rng.nextInt(40);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) {
                sb.append((char) ('a' + rng.nextInt(3)));
            }
            String s = sb.toString();
            int[] z = ZArray.zFunction(s);
            int[] oracle = bruteForceLcp(s);
            assertThat(z).as("串 %s", s).containsExactly(oracle);
        }
    }

    @Test
    void findAllMatchesBruteForceAndOverlaps() {
        assertThat(ZArray.findAll("aaaa", "aa")).containsExactly(0, 1, 2);
        assertThat(ZArray.count("abababab", "abab")).isEqualTo(3);
        Random rng = new Random(7046L);
        for (int round = 0; round < 200; round++) {
            String text = randomString(rng, 60);
            String pattern = randomString(rng, 1 + rng.nextInt(4));
            List<Integer> oracle = new ArrayList<>();
            for (int i = 0; i + pattern.length() <= text.length(); i++) {
                if (text.startsWith(pattern, i)) {
                    oracle.add(i);
                }
            }
            assertThat(ZArray.findAll(text, pattern))
                    .as("text=%s pattern=%s", text, pattern)
                    .containsExactlyElementsOf(oracle);
        }
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> ZArray.zFunction(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ZArray.zFunction("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ZArray.findAll("text", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ZArray.findAll(null, "p")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ZArray.findAll("text", "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ZArray.findAll("a\u0000b", "p")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ZArray.findAll("ab", "p\u0000")).isInstanceOf(IllegalArgumentException.class);
    }

    private int[] bruteForceLcp(String s) {
        int n = s.length();
        int[] z = new int[n];
        z[0] = n;
        for (int i = 1; i < n; i++) {
            int k = 0;
            while (i + k < n && s.charAt(k) == s.charAt(i + k)) {
                k++;
            }
            z[i] = k;
        }
        return z;
    }

    private String randomString(Random rng, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append((char) ('a' + rng.nextInt(3)));
        }
        return sb.toString();
    }
}
