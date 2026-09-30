package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PalindromeTreeTest {

    @Test
    void shouldMatchClassicAnchors() {
        // abba：互异回文 a/b/bb/abba = 4；最长 4
        PalindromeTree abba = PalindromeTree.of("abba");
        assertThat(abba.distinctPalindromeCount()).isEqualTo(4);
        assertThat(abba.longestPalindromeLength()).isEqualTo(4);
        assertThat(abba.allPalindromes()).containsExactly("a", "abba", "b", "bb");
        // aaaa：互异 4（a/aa/aaa/aaaa）；各出现 4/3/2/1
        PalindromeTree aaaa = PalindromeTree.of("aaaa");
        assertThat(aaaa.distinctPalindromeCount()).isEqualTo(4);
        assertThat(aaaa.longestPalindromeLength()).isEqualTo(4);
        assertThat(aaaa.palindromeFrequencies()).containsEntry("a", 4L)
                .containsEntry("aa", 3L).containsEntry("aaa", 2L).containsEntry("aaaa", 1L);
        // 空/单字符
        assertThat(PalindromeTree.of("").distinctPalindromeCount()).isZero();
        assertThat(PalindromeTree.of("").longestPalindromeLength()).isZero();
        assertThat(PalindromeTree.of("x").allPalindromes()).containsExactly("x");
    }

    @Test
    void shouldMatchBruteForceOnRandomTexts() {
        // 暴力圣像：互异回文集与最长长度逐一对拍
        Random random = new Random(131);
        for (int t = 0; t < 30; t++) {
            int n = 1 + random.nextInt(50);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) {
                sb.append((char) ('a' + random.nextInt(3)));
            }
            String text = sb.toString();
            PalindromeTree tree = PalindromeTree.of(text);
            Set<String> palindromes = new HashSet<>();
            int longest = 0;
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j <= n; j++) {
                    String sub = text.substring(i, j);
                    if (new StringBuilder(sub).reverse().toString().equals(sub)) {
                        palindromes.add(sub);
                        longest = Math.max(longest, sub.length());
                    }
                }
            }
            assertThat(tree.distinctPalindromeCount()).as("文本 %d 互异数", t)
                    .isEqualTo((long) palindromes.size());
            assertThat(tree.longestPalindromeLength()).as("文本 %d 最长", t).isEqualTo(longest);
            assertThat(tree.allPalindromes()).containsExactlyInAnyOrderElementsOf(palindromes);
            // 互异回文数 ≤ n（每右端至多新增一回文——eertree 结构界）
            assertThat(tree.distinctPalindromeCount()).isLessThanOrEqualTo(n);
        }
    }

    @Test
    void shouldBeFailFast() {
        assertThatThrownBy(() -> PalindromeTree.of(null))
                .isInstanceOf(IllegalArgumentException.class);
        // 确定性双跑
        PalindromeTree first = PalindromeTree.of("abcba");
        PalindromeTree second = PalindromeTree.of("abcba");
        assertThat(first.allPalindromes()).isEqualTo(second.allPalindromes());
        assertThat(first.allPalindromes()).containsExactlyInAnyOrder("a", "b", "c", "bcb", "abcba");
    }
}
