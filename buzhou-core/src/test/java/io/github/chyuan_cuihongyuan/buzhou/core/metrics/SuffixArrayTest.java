package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6009 / T6220：SuffixArray 合同——静态文本一次建索引
 * 多查询。banana 经典序钉住；随机文本暴力全等；后缀序+
 * LCP 独立复算；fail-fast。
 */
class SuffixArrayTest {

    @Test
    void bananaClassicOrder() {
        SuffixArray suffix = new SuffixArray("banana");
        assertThat(suffix.suffixArray()).containsExactly(5, 3, 1, 0, 4, 2);
        assertThat(suffix.lcpArray()).containsExactly(0, 1, 3, 0, 0, 2);
        assertThat(suffix.contains("ana")).isTrue();
        assertThat(suffix.contains("nab")).isFalse();
        assertThat(suffix.occurrenceCount("ana")).isEqualTo(2);
        assertThat(suffix.occurrenceCount("a")).isEqualTo(3);
    }

    @Test
    void randomQueriesMatchBruteForceOracle() {
        Random rng = new Random(6009L);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            text.append((char) ('a' + rng.nextInt(2)));
        }
        SuffixArray suffix = new SuffixArray(text.toString());
        for (int q = 0; q < 100; q++) {
            StringBuilder sb = new StringBuilder();
            int len = 1 + rng.nextInt(5);
            for (int j = 0; j < len; j++) {
                sb.append((char) ('a' + rng.nextInt(2)));
            }
            String query = sb.toString();
            int brute = 0;
            int idx = text.indexOf(query);
            while (idx >= 0) {
                brute++;
                idx = text.indexOf(query, idx + 1);
            }
            assertThat(suffix.contains(query)).as("contains %s", query)
                    .isEqualTo(text.toString().contains(query));
            assertThat(suffix.occurrenceCount(query)).as("count %s", query).isEqualTo(brute);
        }
    }

    @Test
    void suffixOrderAndLcpIndependentlyVerifiable() {
        Random rng = new Random(42L);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 120; i++) {
            text.append((char) ('a' + rng.nextInt(3)));
        }
        SuffixArray suffix = new SuffixArray(text.toString());
        int[] sa = suffix.suffixArray();
        int[] lcp = suffix.lcpArray();
        String s = text.toString();
        for (int i = 0; i + 1 < sa.length; i++) {
            assertThat(s.substring(sa[i])).as("后缀序 %d", i)
                    .isLessThanOrEqualTo(s.substring(sa[i + 1]));
        }
        for (int trial = 0; trial < 50; trial++) {
            int i = 1 + rng.nextInt(sa.length - 1);
            int a = sa[i - 1];
            int b = sa[i];
            int h = 0;
            while (a + h < s.length() && b + h < s.length() && s.charAt(a + h) == s.charAt(b + h)) {
                h++;
            }
            assertThat(lcp[i]).as("lcp[%d]", i).isEqualTo(h);
        }
        assertThat(suffix.suffixArray()).isNotSameAs(sa);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new SuffixArray(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SuffixArray("")).isInstanceOf(IllegalArgumentException.class);
        SuffixArray suffix = new SuffixArray("abc");
        assertThatThrownBy(() -> suffix.contains(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> suffix.contains("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> suffix.occurrenceCount(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
