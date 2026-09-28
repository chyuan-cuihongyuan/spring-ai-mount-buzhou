package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7038：ChiSquareUniformity 合同——拟合优度统计量。
 * 均匀序列低 χ²/偏斜序列高 χ² 对比钉住；期望/自由度读数；
 * fail-fast。
 */
class ChiSquareUniformityTest {

    @Test
    void uniformVsSkewedContrast() {
        List<Integer> uniform = new ArrayList<>();
        Random rng = new Random(7038L);
        for (int i = 0; i < 10_000; i++) {
            uniform.add(rng.nextInt(10));
        }
        ChiSquareUniformity.Result good = ChiSquareUniformity.uniformity(
                ChiSquareUniformity.bucketize(uniform, 10, 10), 10);
        assertThat(good.chiSquare()).as("均匀序列 χ² ≈ 自由度 9").isLessThan(40);

        List<Integer> skewed = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            skewed.add(i % 2 == 0 ? 0 : 1);
        }
        ChiSquareUniformity.Result bad = ChiSquareUniformity.uniformity(
                ChiSquareUniformity.bucketize(skewed, 10, 10), 10);
        assertThat(bad.chiSquare()).as("偶桶集中 χ² 巨大").isGreaterThan(1000);
        assertThat(bad.degreesOfFreedom()).isEqualTo(9);
    }

    @Test
    void handComputedTwoBuckets() {
        ChiSquareUniformity.Result result = ChiSquareUniformity.uniformity(
                new int[]{0, 0, 0, 1}, 2);
        assertThat(result.chiSquare()).isEqualTo(1.0);
        assertThat(result.degreesOfFreedom()).isEqualTo(1);
        assertThat(result.contributions()).hasSize(2);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> ChiSquareUniformity.uniformity(new int[]{0, 1}, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChiSquareUniformity.uniformity(new int[]{0, 1}, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChiSquareUniformity.uniformity(new int[]{0, 5}, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChiSquareUniformity.uniformity(null, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
