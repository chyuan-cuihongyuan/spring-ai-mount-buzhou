package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 10046 / X10093：DecisionTreeCart 合同验证——一维可分手锚+XOR 深度 2
 * 手锚+纯集叶+无增益转叶+确定性+fail-fast。
 */
class DecisionTreeCartTest {

    private static void assertAllCorrect(double[][] features, int[] labels,
            DecisionTreeCart tree) {
        for (int i = 0; i < features.length; i++) {
            assertThat(tree.predict(features[i])).as("样本 %d", i).isEqualTo(labels[i]);
        }
    }

    @Test
    void shouldSplitSingleThreshold_whenOneDimensionSeparable() {
        double[][] x = {{1}, {2}, {3}, {8}, {9}, {10}};
        int[] y = {0, 0, 0, 1, 1, 1};
        DecisionTreeCart tree = DecisionTreeCart.fit(x, y, 2, 3, 1);
        assertAllCorrect(x, y, tree);
        assertThat(tree.predict(new double[]{4.5})).isEqualTo(0);
        assertThat(tree.predict(new double[]{7.5})).isEqualTo(1);
    }

    @Test
    void shouldFitXor_whenDepthTwoAllowed() {
        double[][] x = {{0, 0}, {0, 1}, {1, 0}, {1, 1}};
        int[] xor = {0, 1, 1, 0};
        DecisionTreeCart tree = DecisionTreeCart.fit(x, xor, 2, 2, 1);
        assertAllCorrect(x, xor, tree);
    }

    @Test
    void shouldStopAtLeaf_whenPureNode() {
        double[][] x = {{1}, {2}, {3}};
        int[] y = {1, 1, 1};
        DecisionTreeCart tree = DecisionTreeCart.fit(x, y, 2, 5, 1);
        for (double[] row : x) {
            assertThat(tree.predict(row)).isEqualTo(1);
        }
    }

    @Test
    void shouldFallBackToMajority_whenNoGainfulSplit() {
        // 同特征值无法分裂——叶回多数类（平局取首类 0）
        double[][] x = {{5}, {5}, {5}, {5}};
        int[] y = {0, 0, 1, 1};
        DecisionTreeCart tree = DecisionTreeCart.fit(x, y, 2, 5, 1);
        assertThat(tree.predict(new double[]{5})).isEqualTo(0);
    }

    @Test
    void shouldReproduceIdenticalPredictions_whenSameInputTwice() {
        double[][] x = {{0, 0}, {0, 1}, {1, 0}, {1, 1}};
        int[] xor = {0, 1, 1, 0};
        DecisionTreeCart first = DecisionTreeCart.fit(x, xor, 2, 2, 1);
        DecisionTreeCart second = DecisionTreeCart.fit(x, xor, 2, 2, 1);
        for (double[] row : x) {
            assertThat(second.predict(row)).isEqualTo(first.predict(row));
        }
    }

    @Test
    void shouldFailFast_whenInvalidInputs() {
        double[][] x = {{1}, {2}};
        assertThatThrownBy(() -> DecisionTreeCart.fit(x, new int[]{0, 2}, 2, 3, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("标签越界");
        assertThatThrownBy(() -> DecisionTreeCart.fit(x, new int[]{0, 1}, 2, 0, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("深度限");
        assertThatThrownBy(() -> DecisionTreeCart.fit(x, new int[]{0, 1}, 2, 3, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("最小叶");
        assertThatThrownBy(() -> DecisionTreeCart.fit(new double[][]{{1, 2}, {1}},
                new int[]{0, 1}, 2, 3, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("等长特征行");
    }
}
