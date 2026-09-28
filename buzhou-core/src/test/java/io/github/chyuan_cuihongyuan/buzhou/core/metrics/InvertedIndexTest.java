package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7019：InvertedIndex 合同——词项→posting 布尔召回。
 * AND/OR 手锚；随机语料 vs 逐文档扫列圣像；幂等替换/
 * 删除统计一致；fail-fast。
 */
class InvertedIndexTest {

    @Test
    void handCorpusAndOrSemantics() {
        InvertedIndex index = new InvertedIndex();
        index.add(1, "the quick brown fox");
        index.add(2, "the lazy brown dog");
        index.add(3, "quick fox jumps");

        assertThat(index.searchAnd("quick", "brown")).containsExactly(1L);
        assertThat(index.searchAnd("quick", "fox")).containsExactly(1L, 3L);
        assertThat(index.searchAnd("the")).containsExactly(1L, 2L);
        assertThat(index.searchOr("lazy", "jumps")).containsExactly(2L, 3L);
        assertThat(index.searchAnd("missing")).isEmpty();
        assertThat(index.documentFrequency("brown")).isEqualTo(2);
        assertThat(index.documentFrequency("the")).isEqualTo(2);
        assertThat(index.termCount()).isEqualTo(7);
        assertThat(index.docCount()).isEqualTo(3);
    }

    @Test
    void idempotentReplaceAndRemove() {
        InvertedIndex index = new InvertedIndex();
        index.add(1, "alpha beta");
        index.add(1, "beta gamma");
        assertThat(index.documentFrequency("alpha")).isZero();
        assertThat(index.documentFrequency("gamma")).isEqualTo(1);
        assertThat(index.searchOr("alpha", "gamma")).containsExactly(1L);
        index.remove(1);
        assertThat(index.docCount()).isZero();
        assertThat(index.termCount()).isZero();
        assertThatThrownBy(() -> index.remove(1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void randomCorpusMatchesBruteScan() {
        InvertedIndex index = new InvertedIndex();
        String[] words = {"alpha", "beta", "gamma", "delta", "epsilon"};
        String[] corpus = new String[40];
        Random rng = new Random(7018L);
        for (int doc = 0; doc < 40; doc++) {
            StringBuilder sb = new StringBuilder();
            for (int w = 0; w < 6; w++) {
                sb.append(words[rng.nextInt(words.length)]).append(' ');
            }
            corpus[doc] = sb.toString();
            index.add(doc, corpus[doc]);
        }
        for (int round = 0; round < 100; round++) {
            String a = words[rng.nextInt(words.length)];
            String b = words[rng.nextInt(words.length)];
            List<Long> andOracle = new java.util.ArrayList<>();
            List<Long> orOracle = new java.util.ArrayList<>();
            for (int doc = 0; doc < 40; doc++) {
                boolean hasA = corpus[doc].contains(a);
                boolean hasB = corpus[doc].contains(b);
                if (hasA && hasB) {
                    andOracle.add((long) doc);
                }
                if (hasA || hasB) {
                    orOracle.add((long) doc);
                }
            }
            assertThat(index.searchAnd(a, b)).containsExactlyElementsOf(andOracle);
            assertThat(index.searchOr(a, b)).containsExactlyElementsOf(orOracle);
        }
    }

    @Test
    void failFastContract() {
        InvertedIndex index = new InvertedIndex();
        assertThatThrownBy(() -> index.add(1, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> index.add(1, "   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> index.searchAnd()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> index.searchAnd(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> index.documentFrequency("  ")).isInstanceOf(IllegalArgumentException.class);
    }
}
