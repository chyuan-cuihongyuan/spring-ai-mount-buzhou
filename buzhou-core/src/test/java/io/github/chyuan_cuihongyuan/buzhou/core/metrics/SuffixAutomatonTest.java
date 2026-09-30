package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SuffixAutomatonTest {

    @Test
    void shouldAnswerMembershipAnchors() {
        SuffixAutomaton sam = SuffixAutomaton.of("ababbc");
        assertThat(sam.containsSubstring("ababbc")).isTrue();
        assertThat(sam.containsSubstring("ab")).isTrue();
        assertThat(sam.containsSubstring("bbc")).isTrue();
        assertThat(sam.containsSubstring("abb")).isTrue();
        assertThat(sam.containsSubstring("")).isTrue();
        assertThat(sam.containsSubstring("aba")).isTrue();
        assertThat(sam.containsSubstring("abc")).isFalse();
        assertThat(sam.containsSubstring("babbc")).isTrue();
        assertThat(sam.containsSubstring("cb")).isFalse();
    }

    @Test
    void shouldCountDistinctAndFrequencyExactly() {
        // 互异子串数锚：aaa→3（a/aa/aaa）、abc→6（全互异）、aab→5
        assertThat(SuffixAutomaton.of("aaa").distinctSubstringCount()).isEqualTo(3);
        assertThat(SuffixAutomaton.of("abc").distinctSubstringCount()).isEqualTo(6);
        assertThat(SuffixAutomaton.of("aab").distinctSubstringCount()).isEqualTo(5);
        // 频次锚：aaaa 中 aa 出现 3 次、a 出现 4 次、aaa 出现 2 次
        SuffixAutomaton sam = SuffixAutomaton.of("aaaa");
        assertThat(sam.substringFrequency("a")).isEqualTo(4);
        assertThat(sam.substringFrequency("aa")).isEqualTo(3);
        assertThat(sam.substringFrequency("aaa")).isEqualTo(2);
        assertThat(sam.substringFrequency("aaaa")).isEqualTo(1);
        assertThat(sam.substringFrequency("aaaaa")).isZero();
        // byLength 总和 = 互异数
        assertThat(sam.substringCountByLength().stream().mapToLong(Long::longValue).sum())
                .isEqualTo(sam.distinctSubstringCount());
    }

    @Test
    void shouldMatchBruteForceOnRandomTexts() {
        // 暴力圣像：随机 4 字母表文本——互异子串数与出现频次逐一对拍
        Random random = new Random(107);
        for (int t = 0; t < 30; t++) {
            int n = 1 + random.nextInt(60);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) {
                sb.append((char) ('a' + random.nextInt(4)));
            }
            String text = sb.toString();
            SuffixAutomaton sam = SuffixAutomaton.of(text);
            Set<String> distinct = new HashSet<>();
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j <= n; j++) {
                    distinct.add(text.substring(i, j));
                }
            }
            assertThat(sam.distinctSubstringCount()).as("文本 %d 互异数", t)
                    .isEqualTo((long) distinct.size());
            for (int q = 0; q < 20; q++) {
                int from = random.nextInt(n);
                int to = from + 1 + random.nextInt(n - from);
                String query = text.substring(from, to);
                long expected = 0;
                for (int i = 0; i + query.length() <= n; i++) {
                    if (text.startsWith(query, i)) {
                        expected++;
                    }
                }
                assertThat(sam.substringFrequency(query)).as("文本 %d 频次 %s", t, query)
                        .isEqualTo(expected);
            }
            // 状态数 ≤ 2n（O(n) 结构界）
            assertThat(sam.stateCount()).isLessThanOrEqualTo(2 * n + 1);
        }
    }

    @Test
    void shouldBeFailFast() {
        SuffixAutomaton sam = SuffixAutomaton.of("abc");
        assertThatThrownBy(() -> SuffixAutomaton.of(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sam.containsSubstring(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sam.substringFrequency(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sam.substringFrequency(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SuffixAutomaton.of("abc€"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("字节域");
    }
}
