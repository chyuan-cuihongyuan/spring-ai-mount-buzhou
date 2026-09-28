package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7027：Bm25Ranker 合同——IDF 饱和+TF 饱和+长度归一。
 * 排序手锚；长度归一效应；并列 canonical；fail-fast。
 */
class Bm25RankerTest {

    @Test
    void rankingHandAnchors() {
        Bm25Ranker ranker = new Bm25Ranker();
        ranker.add(1, "apple banana apple");
        ranker.add(2, "apple cherry");
        ranker.add(3, "banana banana banana date");
        var ranking = ranker.rank("apple", 1.2, 0.75);
        assertThat(ranking).hasSize(3);
        assertThat(ranking.get(0).docId()).isEqualTo(1L);
        assertThat(ranking.get(1).docId()).isEqualTo(2L);
        assertThat(ranking.get(2).score()).isZero();
    }

    @Test
    void shorterDocumentBoostedByLengthNormalization() {
        Bm25Ranker ranker = new Bm25Ranker();
        ranker.add(1, "cat");
        ranker.add(2, "cat dog elephant hippopotamus giraffe");
        var ranking = ranker.rank("cat", 1.2, 0.75);
        assertThat(ranking.get(0).docId()).isEqualTo(1L);
        var noNormalization = ranker.rank("cat", 1.2, 0.0);
        assertThat(noNormalization.get(0).docId())
                .as("b=0 归一关闭——长度增益消失，tf=1 并列按 docId canonical")
                .isEqualTo(1L);
    }

    @Test
    void tiesCanonicalByIdAscending() {
        Bm25Ranker ranker = new Bm25Ranker();
        ranker.add(7, "same same");
        ranker.add(3, "same same");
        ranker.add(5, "same same");
        var ranking = ranker.rank("same", 1.2, 0.75);
        assertThat(ranking).extracting(Bm25Ranker.Scored::docId)
                .containsExactly(3L, 5L, 7L);
        assertThat(ranking.get(0).score()).isEqualTo(ranking.get(2).score());
    }

    @Test
    void failFastContract() {
        Bm25Ranker ranker = new Bm25Ranker();
        assertThatThrownBy(() -> ranker.add(1, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ranker.add(1, "  ")).isInstanceOf(IllegalArgumentException.class);
        ranker.add(1, "word");
        assertThatThrownBy(() -> ranker.rank(null, 1.2, 0.75)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ranker.rank("word", -1, 0.75)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ranker.rank("word", 1.2, 1.5)).isInstanceOf(IllegalArgumentException.class);
    }
}
