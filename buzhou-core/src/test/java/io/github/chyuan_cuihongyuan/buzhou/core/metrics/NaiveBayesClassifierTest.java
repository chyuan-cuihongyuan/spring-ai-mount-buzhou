package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 10042 / X10085：NaiveBayesClassifier 合同验证——双类词袋手锚
 * +未见词平滑+类先验倾斜+确定性+fail-fast。词 ID：0=星球 1=彗星（天文类）
 * 2=股票 3=牛市（财经类）。
 */
class NaiveBayesClassifierTest {

    private static final int[][] TRAIN_DOCS = {
            {0, 1}, {1, 0}, {0, 1, 0},          // 天文
            {2, 3}, {3, 2}, {2, 2, 3}};         // 财经
    private static final int[] TRAIN_LABELS = {0, 0, 0, 1, 1, 1};

    @Test
    void shouldClassifyDiscriminativeWords_whenTwoClasses() {
        NaiveBayesClassifier model = NaiveBayesClassifier.fit(
                TRAIN_DOCS, TRAIN_LABELS, 2);
        assertThat(model.predict(new int[]{0, 1})).isEqualTo(0);
        assertThat(model.predict(new int[]{2, 3})).isEqualTo(1);
        assertThat(model.predict(new int[]{0, 0, 1})).isEqualTo(0);
        assertThat(model.predict(new int[]{3, 3, 2})).isEqualTo(1);
    }

    @Test
    void shouldNotCrashAndStaySane_whenUnseenWordAppears() {
        NaiveBayesClassifier model = NaiveBayesClassifier.fit(
                TRAIN_DOCS, TRAIN_LABELS, 2);
        // 词 9 未见：平滑兜底不炸，判别词仍主导
        assertThat(model.predict(new int[]{9, 0, 1})).isEqualTo(0);
        assertThat(model.predict(new int[]{9, 2, 3})).isEqualTo(1);
    }

    @Test
    void shouldTiltToPrior_whenImbalancedCorpus() {
        // 类 0 六篇、类 1 一篇，共用同一无判别词 0：纯先验倾斜
        int[][] docs = {{0}, {0}, {0}, {0}, {0}, {0}, {0}};
        int[] labels = {0, 0, 0, 0, 0, 0, 1};
        NaiveBayesClassifier model = NaiveBayesClassifier.fit(docs, labels, 2);
        assertThat(model.predict(new int[]{0})).isEqualTo(0);
    }

    @Test
    void shouldReproduceIdenticalPrediction_whenSameInputTwice() {
        NaiveBayesClassifier model = NaiveBayesClassifier.fit(
                TRAIN_DOCS, TRAIN_LABELS, 2);
        assertThat(model.predict(new int[]{0, 1}))
                .isEqualTo(model.predict(new int[]{0, 1}));
    }

    @Test
    void shouldFailFast_whenInvalidInputs() {
        assertThatThrownBy(() -> NaiveBayesClassifier.fit(TRAIN_DOCS, TRAIN_LABELS, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NaiveBayesClassifier.fit(TRAIN_DOCS, new int[]{0, 0, 0, 1, 1, 5}, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("标签越界");
        assertThatThrownBy(() -> NaiveBayesClassifier.fit(
                new int[][]{{0, -1}, {1}}, new int[]{0, 1}, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("词 ID 非负");
        assertThatThrownBy(() -> NaiveBayesClassifier.fit(new int[0][], new int[0], 2))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
