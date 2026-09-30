package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class TspTwoOptTest {

    @Test
    void shouldUncrossTheSquareAnchor() {
        // 单位正方形（输入序制造交叉：0=左下 1=右上 2=左上 3=右下）——2-opt 去交叉收敛周长 4
        double[][] square = {{0, 0}, {1, 1}, {0, 1}, {1, 0}};
        TspTwoOpt.Tour tour = TspTwoOpt.optimize(square, 100);
        assertThat(tour.length()).isCloseTo(4.0, within(1e-9));
        assertThat(TspTwoOpt.lengthOf(square, tour.order())).isCloseTo(tour.length(), within(1e-12));
        // 环序是排列（恰含 0..3 各一次）
        Arrays.sort(tour.order().clone());
        assertThat(tour.order()).containsExactlyInAnyOrder(0, 1, 2, 3);
    }

    @Test
    void shouldNeverWorsenAndStayDeterministic() {
        // 圣像：随机点集 2-opt 长度 ≤ 初始序直行长；确定性双跑
        Random random = new Random(151);
        for (int t = 0; t < 25; t++) {
            int n = 3 + random.nextInt(15);
            double[][] points = new double[n][];
            for (int i = 0; i < n; i++) {
                points[i] = new double[]{random.nextDouble() * 100, random.nextDouble() * 100};
            }
            int[] initial = new int[n];
            for (int i = 0; i < n; i++) {
                initial[i] = i;
            }
            TspTwoOpt.Tour tour = TspTwoOpt.optimize(points, 200);
            assertThat(tour.length()).as("图 %d 不劣化", t)
                    .isLessThanOrEqualTo(TspTwoOpt.lengthOf(points, initial) + 1e-9);
            assertThat(tour.length()).isCloseTo(TspTwoOpt.lengthOf(points, tour.order()), within(1e-9));
            assertThat(TspTwoOpt.optimize(points, 200).order()).containsExactly(tour.order());
        }
    }

    @Test
    void shouldHandleDegenerateAndFailFast() {
        // 两点往返；三点恒为三角环（初始序已优）
        TspTwoOpt.Tour two = TspTwoOpt.optimize(new double[][]{{0, 0}, {3, 4}}, 10);
        assertThat(two.length()).isCloseTo(10.0, within(1e-9)); // 往返 5+5
        TspTwoOpt.Tour three = TspTwoOpt.optimize(new double[][]{{0, 0}, {1, 0}, {0, 1}}, 10);
        assertThat(three.length()).isCloseTo(2 + Math.sqrt(2), within(1e-9));
        // 共线五点：最优为线段往返
        double[][] line = {{0, 0}, {4, 0}, {1, 0}, {3, 0}, {2, 0}};
        assertThat(TspTwoOpt.optimize(line, 100).length()).isCloseTo(8.0, within(1e-9));
        assertThatThrownBy(() -> TspTwoOpt.optimize(null, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TspTwoOpt.optimize(new double[][]{{0, 0}}, 10))
                .hasMessageContaining("≥2");
        assertThatThrownBy(() -> TspTwoOpt.optimize(new double[][]{{0, 0}, {1, 1}}, -1))
                .hasMessageContaining("轮数");
        assertThatThrownBy(() -> TspTwoOpt.optimize(new double[][]{{0, 0}, {1}}, 10))
                .hasMessageContaining("维度");
    }
}
