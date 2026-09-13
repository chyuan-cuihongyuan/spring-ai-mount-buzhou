package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 714 / T1028–T1029：相似度阈值判定器——全等 1.0、无关 0、大小写空白
 * 折叠、阈值边界含等号、越界拒绝、空串退化口径。
 */
class SimilarityEvaluatorTest {

    @Test
    void identicalTextScoresOneAndPasses() {
        EvalScore score = BuiltInEvaluators.similarity(0.8)
                .evaluate("用 Arc 浏览器打开邮件", "用 Arc 浏览器打开邮件", null);
        assertThat(score.passed()).isTrue();
        assertThat(score.detail()).contains("similarity=1.000000").contains("阈值=0.8");
    }

    @Test
    void unrelatedTextScoresLowAndFails() {
        EvalScore score = BuiltInEvaluators.similarity(0.8)
                .evaluate("量子计算原理概述", "今晚吃什么好呢", null);
        assertThat(score.passed()).isFalse();
        assertThat(score.detail()).contains("similarity=0.000000");
    }

    @Test
    void caseWhitespaceAndMinorWordOrderStillScoreHigh() {
        // 大小写 + 空白 + 标点差异：高分但不至于 1.0
        double ratio = BuiltInEvaluators.trigramJaccard(
                "Open the Mail app with Arc!", "open  the mail app with arc");
        assertThat(ratio).isGreaterThan(0.8);

        EvalScore score = BuiltInEvaluators.similarity(0.8)
                .evaluate("Open the Mail app with Arc!", "open the mail app with arc", null);
        assertThat(score.passed()).isTrue();
    }

    @Test
    void thresholdIsInclusiveAtBoundary() {
        // "abcd" trigrams={abc,bcd}；"abc" trigrams={abc} → J = 1/2（可预知构造）
        assertThat(BuiltInEvaluators.trigramJaccard("abcd", "abc")).isEqualTo(0.5);
        EvalScore atThreshold = BuiltInEvaluators.similarity(0.5)
                .evaluate("abcd", "abc", null);
        assertThat(atThreshold.passed()).as("边界含等号").isTrue();
        EvalScore belowThreshold = BuiltInEvaluators.similarity(0.51)
                .evaluate("abcd", "abc", null);
        assertThat(belowThreshold.passed()).isFalse();
    }

    @Test
    void degenerateEmptyCasesAndValidation() {
        assertThat(BuiltInEvaluators.trigramJaccard("", "")).isEqualTo(1.0);
        assertThat(BuiltInEvaluators.trigramJaccard("", "abc")).isZero();
        assertThat(BuiltInEvaluators.trigramJaccard("abc", "")).isZero();
        assertThatThrownBy(() -> BuiltInEvaluators.similarity(1.01))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BuiltInEvaluators.similarity(-0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
