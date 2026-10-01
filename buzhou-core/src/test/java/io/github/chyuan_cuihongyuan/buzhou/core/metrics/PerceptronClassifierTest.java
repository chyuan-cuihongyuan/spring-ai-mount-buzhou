package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 10043 / X10087：PerceptronClassifier 合同验证——AND/OR 门训练集全对
 * +单步更新数值手锚+确定性+fail-fast。
 */
class PerceptronClassifierTest {

    private static final double[][] BINARY_INPUTS = {
            {0, 0}, {0, 1}, {1, 0}, {1, 1}};

    private static void assertAllCorrect(double[][] inputs, int[] labels,
            PerceptronClassifier model) {
        for (int i = 0; i < inputs.length; i++) {
            assertThat(model.predict(inputs[i])).as("样本 %d", i).isEqualTo(labels[i]);
        }
    }

    @Test
    void shouldLearnAndGate_whenLinearlySeparable() {
        int[] andLabels = {0, 0, 0, 1};
        PerceptronClassifier model = PerceptronClassifier.fit(
                BINARY_INPUTS, andLabels, 0.5, 50);
        assertAllCorrect(BINARY_INPUTS, andLabels, model);
    }

    @Test
    void shouldLearnOrGate_whenLinearlySeparable() {
        int[] orLabels = {0, 1, 1, 1};
        PerceptronClassifier model = PerceptronClassifier.fit(
                BINARY_INPUTS, orLabels, 0.5, 50);
        assertAllCorrect(BINARY_INPUTS, orLabels, model);
    }

    @Test
    void shouldMatchManualUpdate_whenSingleSampleEpochs() {
        // 手算锚 A：x=(1,1) y=1，激活 0→ŷ=1 无更新（w=(0,0) b=0）——
        // 零权重下激活恒 0：任意输入皆边界平局归类 1
        PerceptronClassifier noUpdate = PerceptronClassifier.fit(
                new double[][]{{1, 1}}, new int[]{1}, 0.5, 1);
        assertThat(noUpdate.predict(new double[]{0, 0})).isEqualTo(1);
        assertThat(noUpdate.predict(new double[]{-3, -3})).isEqualTo(1);
        // 手算锚 B：x=(1,1) y=0，激活 0→ŷ=1 错分 error=−1 →
        // w=(−0.5,−0.5) b=−0.5：(0,0)→−0.5→0；(4,4)→−4.5→0
        PerceptronClassifier mistake = PerceptronClassifier.fit(
                new double[][]{{1, 1}}, new int[]{0}, 0.5, 1);
        assertThat(mistake.predict(new double[]{0, 0})).isEqualTo(0);
        assertThat(mistake.predict(new double[]{4, 4})).isEqualTo(0);
        assertThat(mistake.predict(new double[]{-4, 0})).isEqualTo(1);
    }

    @Test
    void shouldReproduceIdenticalModel_whenSameInputTwice() {
        int[] labels = {0, 0, 0, 1};
        PerceptronClassifier first = PerceptronClassifier.fit(BINARY_INPUTS, labels, 0.5, 10);
        PerceptronClassifier second = PerceptronClassifier.fit(BINARY_INPUTS, labels, 0.5, 10);
        for (double[] input : BINARY_INPUTS) {
            assertThat(second.predict(input)).isEqualTo(first.predict(input));
        }
    }

    @Test
    void shouldFailFast_whenInvalidInputs() {
        assertThatThrownBy(() -> PerceptronClassifier.fit(BINARY_INPUTS,
                new int[]{0, 0, 2, 1}, 0.5, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("{0,1}");
        assertThatThrownBy(() -> PerceptronClassifier.fit(BINARY_INPUTS,
                new int[]{0, 0, 0, 1}, 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("学习率");
        assertThatThrownBy(() -> PerceptronClassifier.fit(BINARY_INPUTS,
                new int[]{0, 0, 0, 1}, 0.5, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("轮数");
        assertThatThrownBy(() -> PerceptronClassifier.fit(null, null, 0.5, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
